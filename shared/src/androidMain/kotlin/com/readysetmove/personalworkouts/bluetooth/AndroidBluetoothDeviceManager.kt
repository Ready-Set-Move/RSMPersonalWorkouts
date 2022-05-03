package com.readysetmove.personalworkouts.bluetooth

import android.bluetooth.BluetoothGatt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class AndroidBluetoothDeviceManager(override val deviceName: String, val bleGatt: BluetoothGatt) : DeviceManager {
    override fun weightUpdates(): Flow<Int> {
        return (1..10).asFlow()
    }
}