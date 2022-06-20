package com.readysetmove.personalworkouts.device

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MockDeviceStore(private val mainDispatcher: CoroutineContext): IsDeviceStore, CoroutineScope by CoroutineScope(mainDispatcher) {
    private val state = MutableStateFlow(DeviceState())
    private val sideEffect = MutableSharedFlow<DeviceSideEffect>()

    override fun observeState(): StateFlow<DeviceState> = state
    override fun observeSideEffect(): Flow<DeviceSideEffect> = sideEffect

    private var trackingJob: Job? = null
    private val trackedTractions = listOf(
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
    )

    override fun dispatch(action: DeviceAction) {
        when (action) {
            is DeviceAction.StartTracking -> {
                state.value = state.value.copy(trackingActive = true)
                val tractions = trackedTractions.iterator()
                trackingJob = launch {
                    while (tractions.hasNext()) {
                        state.value = state.value.copy(traction = tractions.next().value)
                        delay(500)
                    }
                }
            }
            is DeviceAction.StopTracking -> {
                trackingJob?.cancel()
                state.value = state.value.copy(trackingActive = false, trackedTraction = trackedTractions)
            }
        }
    }
}