package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.bluetooth.BluetoothSideEffect
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DeviceState(
    val traction: Float = 0.0f,
    val trackingActive: Boolean = false,
    val trackedTractions: List<Float> = emptyList(),
) : State

sealed class DeviceAction: Action {
    object StartTracking: DeviceAction()
    object StopTracking: DeviceAction()
}

sealed class DeviceSideEffect : Effect {
    data class Error(val error: Exception) : DeviceSideEffect()
}


class DeviceStore(private val bluetoothStore: BluetoothStore):
    Store<DeviceState, DeviceAction, DeviceSideEffect>,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(DeviceState())
    private val sideEffect = MutableSharedFlow<DeviceSideEffect>()

    override fun observeState(): StateFlow<DeviceState> = state
    override fun observeSideEffect(): Flow<DeviceSideEffect> = sideEffect

    init {
        launch {
            bluetoothStore.observeState().collect { bluetoothState ->
                if (bluetoothState.activeDevice == null) return@collect
                if (!state.value.trackingActive) {
                    state.value = state.value.copy(traction = bluetoothState.traction)
                } else {
                    val trackedTractionsUpdate = state.value.trackedTractions.toMutableList()
                    trackedTractionsUpdate.add(bluetoothState.traction)
                    state.value = state.value.copy(
                        traction = bluetoothState.traction,
                        trackedTractions = trackedTractionsUpdate
                    )
                }
            }
        }
    }

    override fun dispatch(action: DeviceAction) {
        val oldState = state.value

        val newState = when (action) {
            is DeviceAction.StartTracking -> {
                if (!oldState.trackingActive) {
                    oldState.copy(trackingActive = true, trackedTractions = emptyList())
                } else {
                    oldState
                }
            }
            is DeviceAction.StopTracking -> {
                if (oldState.trackingActive) {
                    oldState.copy(trackingActive = false)
                } else {
                    oldState
                }
            }
        }

        if (newState != oldState) {
            state.value = newState
        }
    }

}