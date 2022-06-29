package com.readysetmove.personalworkouts.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothDeviceActions.DisConnected
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*

// TODO: fetch app setup from database
private const val MAX_RECONNECT_ATTEMPTS: Int = 10

class AndroidBluetoothService(private val androidContext: Context) : BluetoothService {
    private val bleAdapter by lazy {
        val bluetoothManager =
            androidContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .build()

    fun getBluetoothEnabled(): Boolean {
        return bleAdapter.isEnabled
    }

    private var sharedFlow: SharedFlow<BluetoothService.BluetoothDeviceActions>? = null
    private var gattInUse: BluetoothGatt? = null

    override fun connectToDevice(
        deviceName: String,
        externalScope: CoroutineScope,
    ): SharedFlow<BluetoothService.BluetoothDeviceActions> {
        // did we already start the flow? then return it for subscribers
        var zeeFlow = sharedFlow
        if (zeeFlow != null) return zeeFlow

        //... otherwise start it to scan, connect and launch the monster callback to listen to the BTLE device
        zeeFlow = startConnectCallback(deviceName).shareIn(externalScope,
            SharingStarted.WhileSubscribed())
        sharedFlow = zeeFlow
        return zeeFlow
    }

    override fun setTara() {
        gattInUse?.let { gatt ->
            val setTaraCharacteristic =
                gatt.getService(serviceUuid)
                    .getCharacteristic(dataUuid)
            setTaraCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            setTaraCharacteristic.value =
                byteArrayOf(100,
                    (1 shr 0).toByte(),
                    (1 shr 8).toByte(),
                    (1 shr 16).toByte(),
                    (1 shr 24).toByte()
                )
            if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                throw BluetoothPermissionNotGrantedException("Could not set tara.")
            }
            gatt.writeCharacteristic(setTaraCharacteristic)
        } ?: throw NotConnectedException("Could not set tara.")
    }

    private fun startConnectCallback(deviceName: String): Flow<BluetoothService.BluetoothDeviceActions> {
        val methodTag = "startConnectCallback"
        return callbackFlow {
            if (!getBluetoothEnabled()) {
                trySendBlocking(DisConnected(BluetoothDisabledException("Bluetooth needs to be enabled to start scanning")))
                close()
                return@callbackFlow
            }

            try {
                val device = scanForDevice(deviceName).single()

                with(device) {
                    val connectionStateChangedCallback = BTLECallback(
                        androidContext = androidContext,
                        maxReconnectAttempts = MAX_RECONNECT_ATTEMPTS,
                        device = device,
                        deviceName = deviceName,
                        rootLogTag = methodTag,
                        onDeviceConnected = {
                            trySendBlocking(BluetoothService.BluetoothDeviceActions.Connected(
                                deviceName = deviceName))
                        },
                        onDisconnect = {
                            disconnect(it)
                        }
                    ) { weight ->
                        trySendBlocking(BluetoothService.BluetoothDeviceActions.WeightChanged(weight))
                    }
                    Log.d("$methodTag.connectionFlow", "Connecting to $address")
                    if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                            Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@callbackFlow disconnect(BluetoothPermissionNotGrantedException(
                            "Needed permissions no longer granted. Device: ${device.address}"))
                    }
                    val currentGatt = connectGatt(androidContext,
                        false,
                        connectionStateChangedCallback,
                        BluetoothDevice.TRANSPORT_LE)
                    gattInUse = currentGatt
                    awaitClose {
                        currentGatt.close()
                        gattInUse = null
                        Log.d("$methodTag.connectionFlow", "Flow closed")
                    }
                }
            } catch (e: CancellationException) {
                val rootCause = e.cause
                if (rootCause is BluetoothService.BluetoothException) {
                    disconnect(rootCause)
                } else {
                    disconnect(ScanFailedException(rootCause?.message
                        ?: "Scan failed with unknown reason."))
                }
            }
        }
    }

    private fun ProducerScope<BluetoothService.BluetoothDeviceActions>.disconnect(
        cause: BluetoothService.BluetoothException,
    ) {
        trySendBlocking(DisConnected(cause))
        close()
        sharedFlow = null
    }

    private fun scanForDevice(deviceName: String): Flow<BluetoothDevice> {
        Log.d("scanForDevice", "Called with deviceName=$deviceName. Creating flow.")
        return callbackFlow {
            Log.d("scanForDevice.scanFlow", "Start scanning for device $deviceName")
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    Log.d("scanForDevice.scanCallback.onScanResult",
                        "Found BLE device address: ${result.device.address}")
                    trySendBlocking(result.device)
                    channel.close()
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.d("scanForDevice.scanCallback.onScanFailed", "Error: $errorCode")
                    cancel(CancellationException("BLE Scan failed",
                        ScanFailedException("Error code: $errorCode")))
                }
            }
            val filters = mutableListOf(ScanFilter.Builder().setDeviceName(deviceName).build())
            if (androidContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                cancel(CancellationException("Missing permissions for scan.",
                    BluetoothPermissionNotGrantedException("BLUETOOTH not granted")))
                return@callbackFlow
            }
            bleAdapter.bluetoothLeScanner.startScan(filters, scanSettings, scanCallback)

            awaitClose {
                Log.d("scanForDevice", "Stopping BLE scanner")
                bleAdapter.bluetoothLeScanner.stopScan(scanCallback)
            }
        }
    }

    companion object {
        val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= 31) listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) else listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
}