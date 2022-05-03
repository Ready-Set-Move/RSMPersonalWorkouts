package com.readysetmove.personalworkouts.bluetooth

import kotlinx.coroutines.flow.Flow

interface DeviceManager {
    val deviceName: String

    fun weightUpdates(): Flow<Int>
}