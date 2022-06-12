package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.state.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockDeviceStore: Store<DeviceState, DeviceAction, DeviceSideEffect> {
    private val state = MutableStateFlow(DeviceState())
    private val sideEffect = MutableSharedFlow<DeviceSideEffect>()

    override fun observeState(): StateFlow<DeviceState> = state
    override fun observeSideEffect(): Flow<DeviceSideEffect> = sideEffect

    override fun dispatch(action: DeviceAction) {
        when (action) {
            is DeviceAction.StartTracking -> {
                state.value = state.value.copy(traction = 8f, trackingActive = true)
            }
            is DeviceAction.StopTracking -> {
                state.value = state.value.copy(trackingActive = false, trackedTraction = listOf(
                    Traction(timestamp = 1000, value = 4f),
                    Traction(timestamp = 2000, value = 5f),
                    Traction(timestamp = 3000, value = 8f),
                    Traction(timestamp = 4000, value = 10f),
                    Traction(timestamp = 4500, value = 20f),
                    Traction(timestamp = 4900, value = 21f),
                    Traction(timestamp = 6000, value = 20.4f),
                    Traction(timestamp = 7000, value = 13.37f),
                    Traction(timestamp = 8000, value = 4.2f),
                    Traction(timestamp = 10000, value = 0f),
                ))
            }
        }
    }
}