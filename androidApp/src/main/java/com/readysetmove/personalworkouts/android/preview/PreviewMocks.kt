package com.readysetmove.personalworkouts.android.preview

import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object PreviewBluetoothService : BluetoothService {
    override fun connectToDevice(deviceName: String): Flow<BluetoothService.BluetoothDeviceActions> {
        return flow {
            BluetoothService.BluetoothDeviceActions.Connected("Preview Device")
        }
    }
}