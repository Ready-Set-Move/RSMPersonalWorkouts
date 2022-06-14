package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store

data class DeviceState(
    val traction: Float = 0.0f,
    val trackingActive: Boolean = false,
    val trackedTraction: List<Traction> = emptyList(),
) : State

sealed class DeviceAction: Action {
    object StartTracking: DeviceAction()
    object StopTracking: DeviceAction()
}

sealed class DeviceSideEffect : Effect {
    data class Error(val error: Exception) : DeviceSideEffect()
}

interface IsDeviceStore: Store<DeviceState, DeviceAction, DeviceSideEffect> {
}