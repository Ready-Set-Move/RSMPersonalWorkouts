package com.readysetmove.personalworkouts.bluetooth

import android.Manifest
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidBluetoothService(private val androidContext: Context) : BluetoothService {
    private val bleScanner by lazy {
        val bluetoothManager =
            androidContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter.bluetoothLeScanner
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .build()

    private var scanInProgress: Boolean = false

    override fun scanForDevice(deviceName: String): Flow<Device> {
        if (scanInProgress) {
            throw ScanInProgressException(
                "Do not start multiple scans. Cancel running attempt first.")
        }
        Log.d("scanForDevice", "Called with deviceName=$deviceName. Creating flow.")
        return callbackFlow {
            Log.d("scanForDevice.connectFlow", "Start scanning for device $deviceName")
            scanInProgress = true
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    with(result.device) {
                        if (androidContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            throw BluetoothConnectPermissionNotGrantedException("BLUETOOTH_CONNECT not granted")
                        }
                        Log.d("scanForDevice.connectFlow.onScanResult",
                            "Found BLE device!Attributes.Name: ${name ?: "Unnamed"}, address: $address")
                        trySendBlocking(Device(name = deviceName, address = result.device.address))
                        channel.close()
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.d("scanForDevice.connectFlow.onScanFailed", "Error: $errorCode")
                    cancel(CancellationException("BLE Scan failed",
                        ScanFailedException("Error code: $errorCode")))
                }
            }
            val filters = mutableListOf(ScanFilter.Builder().setDeviceName(deviceName).build())
            if (androidContext.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                throw BluetoothPermissionNotGrantedException("BLUETOOTH not granted")
            }
            bleScanner.startScan(filters, scanSettings, scanCallback)

            awaitClose {
                Log.d("scanForDevice", "Stopping BLE scanner")
                bleScanner.stopScan(scanCallback)
                scanInProgress = false
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