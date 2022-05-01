package com.readysetmove.personalworkouts.bluetooth

interface BluetoothService {

    class ScanFailedException(message: String) : Throwable(message)

    suspend fun scanForDevice(deviceName: String): Device

    fun stopScan()
}