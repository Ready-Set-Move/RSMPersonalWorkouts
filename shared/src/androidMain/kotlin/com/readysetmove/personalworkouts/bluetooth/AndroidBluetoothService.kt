package com.readysetmove.personalworkouts.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.ScanFailedException
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.ScanInProgressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidBluetoothService(private val adapter: BluetoothAdapter) : BluetoothService {
    private val bleScanner by lazy {
        adapter.bluetoothLeScanner
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .build()

    private var scanInProgress: Boolean = false

    @SuppressLint("MissingPermission")
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
                @SuppressLint("MissingPermission")
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    with(result.device) {
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
            bleScanner.startScan(filters, scanSettings, scanCallback)

            awaitClose {
                Log.d("scanForDevice", "Stopping BLE scanner")
                bleScanner.stopScan(scanCallback)
                scanInProgress = false
            }
        }
    }
}