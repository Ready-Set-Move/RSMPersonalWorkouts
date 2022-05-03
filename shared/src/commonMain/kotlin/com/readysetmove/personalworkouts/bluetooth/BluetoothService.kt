package com.readysetmove.personalworkouts.bluetooth

import kotlinx.coroutines.flow.Flow

interface BluetoothService {

    sealed class BluetoothDeviceActions {
        data class Connected(val deviceName: String) : BluetoothDeviceActions()
        object DisConnected : BluetoothDeviceActions()
        data class WeightChanged(val weight: Float) : BluetoothDeviceActions()
    }

    sealed class BluetoothException(message: String) : Exception(message) {
        class ScanFailedException(message: String) : BluetoothException(message)
        class ConnectFailedException(message: String) : BluetoothException(message)
        class BluetoothDisabledException(message: String) : BluetoothException(message)
        class BluetoothPermissionNotGrantedException(message: String) : BluetoothException(message)
        class BluetoothConnectPermissionNotGrantedException(message: String) :
            BluetoothException(message)
    }

    fun connectToDevice(deviceName: String): Flow<BluetoothDeviceActions>
}