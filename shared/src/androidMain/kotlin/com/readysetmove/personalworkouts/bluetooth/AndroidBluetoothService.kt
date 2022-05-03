package com.readysetmove.personalworkouts.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.*
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.single

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

    override fun connectToDevice(deviceName: String): Flow<DeviceManager?> {
        if (androidContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw BluetoothConnectPermissionNotGrantedException("BLUETOOTH_CONNECT not granted")
        }
        if (!getBluetoothEnabled()) {
            throw BluetoothDisabledException("Bluetooth needs to be enabled to start scanning")
        }

        return callbackFlow {
            var device = scanForDevice(deviceName).single()

            with(device) {
                val connectionStateChangedCallback = object : BluetoothGattCallback() {
                    var reconnectAttemptsLeft = 10
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int,
                    ) {
                        Log.d("scanForDevice.connectionStateChangedCallback",
                            "state changed: status=$status newState=$newState")
                        if (gatt == null) return
                        if (status != GATT_SUCCESS) {
                            Log.d("scanForDevice.connectionStateChangedCallback",
                                "status was failed: $status")
                            if (androidContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                cancel(CancellationException(
                                    "Needed permissions no longer granted.",
                                    ConnectFailedException("Device: ${device.address}"))
                                )
                            }
                            gatt.close()

                            if (status == GATT_INSUFFICIENT_ENCRYPTION || status == GATT_INSUFFICIENT_AUTHENTICATION) {
                                Log.d("scanForDevice.connectionStateChangedCallback",
                                    "status failed with insufficient encryption or authentication. Trying to bond before attempting connection.")
                                // TODO: bond
                            }

                            if (reconnectAttemptsLeft == 0) {
                                cancel(CancellationException(
                                    "Too many gatt connection attempts failed",
                                    ConnectFailedException("Device: $address"))
                                )
                            }
                            // try to reconnect
                            reconnectAttemptsLeft--
                            connectGatt(androidContext,
                                false,
                                this,
                                BluetoothDevice.TRANSPORT_LE)
                        }
                        if (newState == STATE_CONNECTED) {
                            Log.d("scanForDevice.connectionStateChangedCallback",
                                "device connected: $deviceName@$address")
                            trySendBlocking(AndroidBluetoothDeviceManager(deviceName = deviceName,
                                bleGatt = gatt))
                        }
                        if (newState == STATE_DISCONNECTED) {
                            Log.d("scanForDevice.connectionStateChangedCallback",
                                "device disconnected: $deviceName@$address")
                            trySendBlocking(null)
                                .onSuccess {
                                    Log.d("###", "success")
                                }
                                .onFailure {
                                    Log.d("###", "fail")
                                }
                                .onClosed {
                                    Log.d("###", "closed")
                                }
                            Log.d("###", "closing channel")
                            gatt.close()
                            close()
                        }
                    }
                }
                Log.d("scanForDevice.connectionFlow", "Connecting to $address")
                connectGatt(androidContext,
                    false,
                    connectionStateChangedCallback,
                    BluetoothDevice.TRANSPORT_LE)
                awaitClose {
                    Log.d("scanForDevice.connectionFlow", "Flow closed")
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