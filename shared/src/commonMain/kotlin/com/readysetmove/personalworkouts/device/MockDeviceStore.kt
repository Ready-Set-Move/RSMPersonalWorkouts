package com.readysetmove.personalworkouts.device

import kotlinx.coroutines.CoroutineScope
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
    override fun observeSideEffect(): Flow<DeviceSideEffect> = sideEffect

    override fun observeState(): StateFlow<DeviceState> = state

    private val tractions = listOf(
        Traction(timestamp = 1000, value = 4f),
        Traction(timestamp = 2000, value = 5f),
        Traction(timestamp = 2100, value = 8f),
        Traction(timestamp = 2200, value = 10f),
        Traction(timestamp = 2300, value = 30f),
        Traction(timestamp = 2400, value = 40f),
        Traction(timestamp = 3000, value = 45f),
        Traction(timestamp = 4000, value = 48f),
        Traction(timestamp = 4900, value = 50f),
        Traction(timestamp = 6000, value = 55.4f),
        Traction(timestamp = 7000, value = 45.37f),
        Traction(timestamp = 8000, value = 40.2f),
        Traction(timestamp = 10000, value = 35f),
        Traction(timestamp = 11000, value = 32f),
        Traction(timestamp = 12000, value = 25f),
        Traction(timestamp = 13000, value = 20f),
        Traction(timestamp = 14000, value = 15f),
        Traction(timestamp = 15000, value = 0f),
    )

    init {
        launch {
            while(true) {
                val iterator = tractions.iterator()
                while (iterator.hasNext()) {
                    state.value = state.value.copy(traction = iterator.next().value)
                    delay(500)
                }
            }
        }
    }

    override fun dispatch(action: DeviceAction) {
        TODO("Not yet implemented")
    }
}