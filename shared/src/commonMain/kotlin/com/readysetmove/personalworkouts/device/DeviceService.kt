package com.readysetmove.personalworkouts.device

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface DeviceService<ActionType> {
    fun connectToDevice(
        deviceName: String,
        externalScope: CoroutineScope,
    ): Flow<ActionType>

    fun setTara()
    fun calibrate()
    fun readSettings()
}