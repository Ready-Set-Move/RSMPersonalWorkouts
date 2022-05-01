package com.readysetmove.personalworkouts.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidBluetoothService(private val adapter: BluetoothAdapter) : BluetoothService {
    private val bleScanner by lazy {
        adapter.bluetoothLeScanner
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .build()

    private var scanCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    override suspend fun scanForDevice(deviceName: String): Device {
        val btleDevice = suspendCancellableCoroutine<BluetoothDevice> { continuation ->
            Log.d("scanForDevice", "Start scanning for device $deviceName")
            scanCallback = object : ScanCallback() {
                @SuppressLint("MissingPermission")
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    with(result.device) {
                        continuation.resume(result.device)
                        Log.d("ScanCallback",
                            "Found BLE device!Attributes.Name: ${name ?: "Unnamed"}, address: $address")

                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.d("onScanFailed", "Error: $errorCode")
                    continuation.cancel(BluetoothService.ScanFailedException("BTLE Scan failed with code: $errorCode"))
                }
            }
            val filters = mutableListOf(ScanFilter.Builder().setDeviceName(deviceName).build())
            bleScanner.startScan(filters, scanSettings, scanCallback)
            continuation.invokeOnCancellation {
                Log.d("scanForDevice", it?.message ?: "No reason provided")
                stopScan()
            }
        }
        Log.d("scanForDevice", "Device found")
//        return Device(name = deviceName)
        return Device(name = btleDevice.address)
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        if (scanCallback == null) {
            Log.d("stopScanning", "no scan in progress")
            return
        }

        Log.d("stopScanning", "scan in progress: stopping")
        bleScanner.stopScan(scanCallback)
    }
}