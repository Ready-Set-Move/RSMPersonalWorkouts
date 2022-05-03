package com.readysetmove.personalworkouts.android.preview

import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.bluetooth.DeviceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow

object PreviewDeviceManager : DeviceManager {
    override val deviceName: String = "Dev0"

    override fun weightUpdates(): Flow<Int> {
        return (1..10).asFlow()
    }
}

object PreviewBluetoothService : BluetoothService {
    override fun connectToDevice(deviceName: String): Flow<DeviceManager?> {
        return flow {
            PreviewDeviceManager
        }
    }
}