package com.readysetmove.personalworkouts.bluetooth

import kotlinx.coroutines.flow.Flow

interface BluetoothService {

    sealed class BluetoothException(message: String): Throwable(message) {
        class ScanFailedException(message: String) : BluetoothException(message)
        class ScanInProgressException(message: String) : BluetoothException(message)
    }

    fun scanForDevice(deviceName: String): Flow<Device>
}