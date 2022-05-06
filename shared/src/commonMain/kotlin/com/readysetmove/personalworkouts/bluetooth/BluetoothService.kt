package com.readysetmove.personalworkouts.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface BluetoothService {

    sealed class BluetoothException(message: String) : Exception(message) {
        class ScanFailedException(message: String) : BluetoothException(message)
        class ConnectFailedException(message: String) : BluetoothException(message)
        class BluetoothDisabledException(message: String) : BluetoothException(message)
        class BluetoothPermissionNotGrantedException(message: String) : BluetoothException(message)
        class BluetoothConnectPermissionNotGrantedException(message: String) :
            BluetoothException(message)
        class NotConnectedException(message: String) : BluetoothException(message)
    }

    sealed class BluetoothDeviceActions {
        data class Connected(val deviceName: String) : BluetoothDeviceActions()
        data class DisConnected(val cause: BluetoothException) : BluetoothDeviceActions()
        data class WeightChanged(val weight: Float) : BluetoothDeviceActions()
    }

    fun connectToDevice(
        deviceName: String,
        externalScope: CoroutineScope,
    ): Flow<BluetoothDeviceActions>

    fun setTara()
}