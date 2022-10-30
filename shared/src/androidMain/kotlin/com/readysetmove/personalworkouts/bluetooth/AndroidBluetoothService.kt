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
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.*
import com.readysetmove.personalworkouts.device.ConnectionConfiguration
import com.readysetmove.personalworkouts.device.DeviceChange
import com.readysetmove.personalworkouts.device.IsDisconnectCause
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*

// TODO: fetch app setup from database
private const val MAX_RECONNECT_ATTEMPTS: Int = 10

private const val classLogTag = "AndroidBluetoothService"

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

    private var sharedFlow: SharedFlow<DeviceChange>? = null
    private var gattInUse: BluetoothGatt? = null

    override fun connectToDevice(
        connectionConfiguration: ConnectionConfiguration.BLEConnection,
        externalScope: CoroutineScope
    ): Flow<DeviceChange> {
        // did we already start the flow? then return it for subscribers
        var zeeFlow = sharedFlow
        if (zeeFlow != null) return zeeFlow

        //... otherwise start it to scan, connect and launch the monster callback to listen to the BTLE device
        zeeFlow = startConnectCallback(connectionConfiguration).shareIn(externalScope,
            SharingStarted.WhileSubscribed())
        sharedFlow = zeeFlow
        return zeeFlow
    }

    private fun writeCharacteristic(value: Byte, payload: Int = 1) {
        gattInUse?.let { gatt ->
            Napier.d(tag = classLogTag) { "Writing $value to data characteristic" }

            // in some cases this happens and we can't recover from that by reconnect
            // the app needs to be restarted here
            val service = gatt.getService(serviceUuid)
                ?: throw ConnectionBrokenException("Service was null. Could not write characteristic: $value. App needs restart.")
            val characteristic = service.getCharacteristic(sendUuid)
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value =
                byteArrayOf(value)
                    .plus(payload.toString().toByteArray())
            if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                throw BluetoothPermissionNotGrantedException("Could not write characteristic: $value.")
            }
            gatt.writeCharacteristic(characteristic)
        } ?: throw NotConnectedException("Could not write characteristic: $value.")
    }

    override fun setTara() {
        Napier.d(tag = classLogTag) { "Set Tara" }
        writeCharacteristic(setTara)
    }

    override fun calibrate() {
        Napier.d(tag = classLogTag) { "Calibrate" }
        writeCharacteristic(calibrate, payload = 16)
//        writeCharacteristic(setScaleFactor, 0)
//        writeCharacteristic(setAutoTara, payload = 1)
    }

    override fun readSettings() {
        Napier.d(tag = classLogTag) { "Read Settings" }
        writeCharacteristic(readAll)
//        writeCharacteristic(getWiFiStatus)
    }

    private fun startConnectCallback(connectionConfiguration: ConnectionConfiguration.BLEConnection): Flow<DeviceChange> {
        val methodTag = "$classLogTag.startConnectCallback"
        return callbackFlow {
            if (!getBluetoothEnabled())
                return@callbackFlow disconnect(BluetoothDisabledException("Bluetooth needs to be enabled to start scanning"))

            try {
                val device = scanForDevice(connectionConfiguration.deviceName).single()

                with(device) {
                    val connectionStateChangedCallback = BLECallback(
                        androidContext = androidContext,
                        maxReconnectAttempts = MAX_RECONNECT_ATTEMPTS,
                        device = device,
                        deviceName = connectionConfiguration.deviceName,
                        rootLogTag = methodTag,
                        onDeviceConnected = {
                            trySendBlocking(DeviceChange.Connected(
                                connectionConfiguration = connectionConfiguration))
                        },
                        onDisconnect = {
                            disconnect(it)
                        },
                        onDeviceDataReceived = {
                            trySendBlocking(DeviceChange.DeviceDataChanged(deviceConfiguration = it))
                        }
                    ) { weight ->
                        trySendBlocking(DeviceChange.WeightChanged(weight))
                    }
                    Napier.d(tag = "$methodTag.connectionFlow") { "Connecting to $address" }
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
                        currentGatt.disconnect()
                        currentGatt.close()
                        gattInUse = null
                        Napier.d(tag = "$methodTag.connectionFlow") { "Flow closed" }
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

    private fun ProducerScope<DeviceChange>.disconnect(
        cause: IsDisconnectCause,
    ) {
        trySendBlocking(DeviceChange.Disconnected(cause))
        close()
        sharedFlow = null
    }

    private fun scanForDevice(deviceName: String): Flow<BluetoothDevice> {
        val methodLogTag = "$classLogTag.scanForDevice"
        Napier.d(tag = methodLogTag) { "Called with deviceName=$deviceName. Creating flow." }
        return callbackFlow {
            Napier.d(tag = methodLogTag) { "Start scanning for device $deviceName" }
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    Napier.d(tag = "$methodLogTag.onScanResult") { "Found BLE device address: ${result.device.address}" }
                    trySendBlocking(result.device)
                    channel.close()
                }

                override fun onScanFailed(errorCode: Int) {
                    Napier.d(tag = "$methodLogTag.onScanFailed") { "Error: $errorCode" }
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
                Napier.d(tag = methodLogTag) { "Stopping BLE scanner" }
                bleAdapter.bluetoothLeScanner.stopScan(scanCallback)
            }
        }
    }

    companion object {
        val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= 33) listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        else if (Build.VERSION.SDK_INT >= 31) listOf(
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