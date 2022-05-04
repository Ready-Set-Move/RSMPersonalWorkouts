package com.readysetmove.personalworkouts.bluetooth

import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothGatt.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothDeviceActions.Connected
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothDeviceActions.DisConnected
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.single
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

private fun BluetoothGatt.printGattTable() {
    if (services.isEmpty()) {
        Log.i("printGattTable",
            "No service and characteristic available, call discoverServices() first?")
        return
    }
    services.forEach { service ->
        val characteristicsTable = service.characteristics.joinToString(
            separator = "\n|--",
            prefix = "|--"
        ) { it.uuid.toString() }
        Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
        )
    }
}

private val cccdUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
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

    override fun connectToDevice(deviceName: String): Flow<BluetoothService.BluetoothDeviceActions> {
        if (!getBluetoothEnabled()) {
            throw BluetoothDisabledException("Bluetooth needs to be enabled to start scanning")
        }

        val methodTag = "connectToDevice"
        return callbackFlow {
            val device = scanForDevice(deviceName).single()

            with(device) {
                val connectionStateChangedCallback = object : BluetoothGattCallback() {
                    var reconnectAttemptsLeft = MAX_RECONNECT_ATTEMPTS
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int,
                    ) {
                        val logTag = "$methodTag.onConnectionStateChange"
                        Log.d(logTag, "state changed: status=$status newState=$newState")
                        if (gatt == null) return
                        if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                                Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            cancel(CancellationException(
                                "Needed permissions no longer granted.",
                                ConnectFailedException("Device: ${device.address}"))
                            )
                            return
                        }
                        if (status != GATT_SUCCESS) {
                            reconnect(status = status, gatt = gatt, logTag)
                            return
                        }
                        if (newState == STATE_CONNECTED) {
                            reconnectAttemptsLeft = MAX_RECONNECT_ATTEMPTS
                            Log.d(logTag, "device connected: $deviceName@$address")
                            gatt.discoverServices()
                        }
                        if (newState == STATE_DISCONNECTED) {
                            // TODO: should we try auto reconnect here?
                            Log.d(logTag, "device disconnected: $deviceName@$address")
                            trySendBlocking(DisConnected)
                            gatt.close()
                            close()
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                        val logTag = "$methodTag.onServicesDiscovered"
                        if (gatt == null) return
                        if (status != GATT_SUCCESS) {
                            reconnect(status = status, gatt = gatt, logTag)
                        } else {
                            Log.d(logTag, "services discovered")
                            gatt.printGattTable()
                            enableTractionNotifications(gatt, methodTag)
                        }
                    }

                    override fun onCharacteristicChanged(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                    ) {
                        val logTag = "$methodTag.onCharacteristicChanged"
                        Log.d(logTag, "started")
                        if (characteristic == null) return
                        val weight =
                            ByteBuffer.wrap(characteristic.value).order(ByteOrder.LITTLE_ENDIAN)
                                .float
                        trySendBlocking(BluetoothService.BluetoothDeviceActions.WeightChanged(weight = weight))
                    }

                    override fun onDescriptorWrite(
                        gatt: BluetoothGatt?,
                        descriptor: BluetoothGattDescriptor?,
                        status: Int,
                    ) {
                        val logTag = "$methodTag.onDescriptorWrite"
                        Log.d(logTag, "received ${descriptor?.uuid}")
                        if (gatt == null || descriptor?.uuid != cccdUuid) return
                        if (status != GATT_SUCCESS) {
                            reconnect(status = status, gatt = gatt, logTag)
                            return
                        }
                        Log.d(logTag, "CCC descriptor successfully enabled")
                        trySendBlocking(Connected(deviceName = deviceName))
                    }

                    private fun enableTractionNotifications(gatt: BluetoothGatt, logTag: String?) {
                        if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                                Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            cancel(CancellationException(
                                "Needed permissions no longer granted.",
                                ConnectFailedException("Device: ${device.address}"))
                            )
                            return
                        }
                        val tractionCharacteristic =
                            gatt.getService(UUID.fromString("87811010-b3ba-4255-95cc-838c34d33583"))
                                .getCharacteristic(UUID.fromString("0000aa02-0000-1000-8000-00805f9b34fb"))

                        tractionCharacteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
                            if (!gatt.setCharacteristicNotification(tractionCharacteristic, true)) {
                                cancel(CancellationException(
                                    "setCharacteristicNotification failed for traction",
                                    ConnectFailedException("Characteristic: $tractionCharacteristic"))
                                )
                                return
                            }

                            cccDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(cccDescriptor)
                            Log.d(logTag, "traction characteristic enabled")
                        } ?: cancel(CancellationException(
                            "Traction characteristic does not contain CCC descriptor.",
                            ConnectFailedException("Device: ${device.address}"))
                        )
                    }

                    private fun reconnect(status: Int, gatt: BluetoothGatt, logTag: String) {
                        val reconnectMethodTag = "reconnect"
                        Log.d("$logTag.$reconnectMethodTag", "status was failed: $status.")
                        if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                                Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            cancel(CancellationException(
                                "Needed permissions no longer granted.",
                                ConnectFailedException("Device: ${device.address}"))
                            )
                            return
                        }
                        gatt.close()

                        if (status == GATT_INSUFFICIENT_ENCRYPTION || status == GATT_INSUFFICIENT_AUTHENTICATION) {
                            Log.d("$logTag.$reconnectMethodTag",
                                "status failed with insufficient encryption or authentication. Trying to bond before attempting connection.")
                            // TODO: bond
                        }

                        if (reconnectAttemptsLeft == 0) {
                            Log.d("$logTag.$reconnectMethodTag",
                                "Reconnect failed to many times. Stop retry.")
                            cancel(CancellationException(
                                "Too many gatt connection attempts failed",
                                ConnectFailedException("Device: $address"))
                            )
                            return
                        }
                        // TODO: send action to notify user
                        Log.d("$logTag.$methodTag", "Trying to reconnect.")
                        reconnectAttemptsLeft--
                        connectGatt(androidContext,
                            false,
                            this,
                            BluetoothDevice.TRANSPORT_LE)
                    }
                }
                Log.d("$methodTag.connectionFlow", "Connecting to $address")
                val gatt = connectGatt(androidContext,
                    false,
                    connectionStateChangedCallback,
                    BluetoothDevice.TRANSPORT_LE)
                awaitClose {
                    gatt.disconnect()
                    Log.d("$methodTag.connectionFlow", "Flow closed")
                }
            }
        }
    }

    private fun scanForDevice(deviceName: String): Flow<BluetoothDevice> {
        if (androidContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            throw BluetoothPermissionNotGrantedException("BLUETOOTH not granted")
        }
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