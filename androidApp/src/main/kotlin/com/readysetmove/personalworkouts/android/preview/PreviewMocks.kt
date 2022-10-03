package com.readysetmove.personalworkouts.android.preview

import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothDeviceActions.Connected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object PreviewBluetoothService : BluetoothService {
    override fun connectToDevice(deviceName: String, externalScope: CoroutineScope): Flow<BluetoothService.BluetoothDeviceActions> {
        return flow {
            Connected("Preview Device")
        }
    }

    override fun setTara() {}
    override fun calibrate() {}
    override fun readSettings() {}
}