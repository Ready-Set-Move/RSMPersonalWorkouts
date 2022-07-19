package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.IsTimestampProvider
import com.readysetmove.personalworkouts.bluetooth.BluetoothAction
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

// TODO: remove tracking but add management of different devices
class DeviceStore(
    private val bluetoothStore: BluetoothStore,
    private val mainDispatcher: CoroutineContext,
    private val timestampProvider: IsTimestampProvider,
):
    IsDeviceStore,
    CoroutineScope by CoroutineScope(mainDispatcher) {

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
                    val newTraction = Traction(
                        timestamp = timestampProvider.getTimeMillis(),
                        value = bluetoothState.traction,
                    )
                    val trackedTractionsUpdate = state.value.trackedTraction.toMutableList()
                    trackedTractionsUpdate.add(newTraction)
                    state.value = state.value.copy(
                        traction = bluetoothState.traction,
                        trackedTraction = trackedTractionsUpdate
                    )
                }
            }
        }
        bluetoothStore.dispatch(BluetoothAction.ScanAndConnect)
    }

    override fun dispatch(action: DeviceAction) {
        when (action) {
            is DeviceAction.StartTracking -> {
                if (!state.value.trackingActive) {
                    state.value = state.value.copy(trackingActive = true, trackedTraction = emptyList())
                }
            }
            is DeviceAction.StopTracking -> {
                if (state.value.trackingActive) {
                    state.value = state.value.copy(trackingActive = false)
                }
            }
        }
    }

}