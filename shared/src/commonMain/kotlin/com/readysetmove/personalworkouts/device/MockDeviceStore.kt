package com.readysetmove.personalworkouts.device

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MockDeviceStore(private val mainDispatcher: CoroutineContext): IsDeviceStore, CoroutineScope by CoroutineScope(mainDispatcher) {
    private val state = MutableStateFlow(DeviceState())

    override fun observeState(): StateFlow<DeviceState> = state

    private val tractions = listOf(
        Traction(timestamp = 1000, value = 4f),
        Traction(timestamp = 4500, value = 200f),
        Traction(timestamp = 2000, value = 5f),
        Traction(timestamp = 3000, value = 8f),
        Traction(timestamp = 4000, value = 10f),
        Traction(timestamp = 4900, value = 21f),
        Traction(timestamp = 6000, value = 20.4f),
        Traction(timestamp = 7000, value = 13.37f),
        Traction(timestamp = 8000, value = 4.2f),
        Traction(timestamp = 10000, value = 0f),
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