package com.readysetmove.personalworkouts.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.*

val cccdUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
val serviceUuid: UUID = UUID.fromString("87811010-b3ba-4255-95cc-838c34d33583")
val dataUuid: UUID = UUID.fromString("0000aa01-0000-1000-8000-00805f9b34fb")
val tractionUuid: UUID = UUID.fromString("0000aa02-0000-1000-8000-00805f9b34fb")
val sendUuid: UUID = UUID.fromString("0000aa03-0000-1000-8000-00805f9b34fb")

const val setTara: Byte = 100
const val setAutoTara: Byte = 104
const val setScaleFactor: Byte = 110
const val resetScaleFactor: Byte = 112
const val calibrate: Byte = 115
const val readAll: Byte = 117
const val getWiFiStatus: Byte = 78
const val getWiFiStartMode: Byte = 74
const val getAverage: Byte = 120

interface BluetoothService {

    sealed class BluetoothException(message: String) : Exception(message) {
        class ScanFailedException(message: String) : BluetoothException(message)
        class ConnectFailedException(message: String) : BluetoothException(message)
        class BluetoothDisabledException(message: String) : BluetoothException(message)
        class BluetoothPermissionNotGrantedException(message: String) : BluetoothException(message)
        class BluetoothConnectPermissionNotGrantedException(message: String) :
            BluetoothException(message)

        class NotConnectedException(message: String) : BluetoothException(message)
        class ConnectionBrokenException(message: String) : BluetoothException(message)
    }

    sealed class BluetoothDeviceActions {
        data class Connected(val deviceName: String) : BluetoothDeviceActions()
        data class DisConnected(val cause: BluetoothException) : BluetoothDeviceActions()
        data class WeightChanged(val traction: Float) : BluetoothDeviceActions()
    }

    fun connectToDevice(
        deviceName: String,
        externalScope: CoroutineScope,
    ): Flow<BluetoothDeviceActions>

    fun setTara()
    fun calibrate()
    fun readSettings()
}