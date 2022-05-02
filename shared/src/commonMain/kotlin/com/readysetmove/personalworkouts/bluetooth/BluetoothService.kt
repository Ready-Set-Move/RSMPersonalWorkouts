package com.readysetmove.personalworkouts.bluetooth

import kotlinx.coroutines.flow.Flow

interface BluetoothService {

    sealed class BluetoothException(message: String): Exception(message) {
        class ScanFailedException(message: String) : BluetoothException(message)
        class BluetoothDisabledException(message: String) : BluetoothException(message)
        class BluetoothPermissionNotGrantedException(message: String) : BluetoothException(message)
        class BluetoothConnectPermissionNotGrantedException(message: String) : BluetoothException(message)
        class ScanInProgressException(message: String) : BluetoothException(message)
    }

    fun scanForDevice(deviceName: String): Flow<Device>
}