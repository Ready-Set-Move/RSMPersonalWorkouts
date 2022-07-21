package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.SimpleStore
import com.readysetmove.personalworkouts.state.State

data class DeviceState(
    val traction: Float = 0.0f,
) : State

sealed class DeviceAction: Action

interface IsDeviceStore: SimpleStore<DeviceState, DeviceAction> {
}