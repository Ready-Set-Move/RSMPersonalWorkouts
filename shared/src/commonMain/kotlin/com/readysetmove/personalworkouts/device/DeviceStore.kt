package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.bluetooth.BluetoothAction
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

// TODO: add management of different devices
class DeviceStore(
    private val bluetoothStore: BluetoothStore,
    private val mainDispatcher: CoroutineContext,
):
    IsDeviceStore,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(DeviceState())

    override fun observeState(): StateFlow<DeviceState> = state

    init {
        launch {
            bluetoothStore.observeState().collect { bluetoothState ->
                if (bluetoothState.activeDevice == null) return@collect
                state.value = state.value.copy(traction = bluetoothState.traction, deviceConfiguration = bluetoothState.deviceConfiguration)
            }
        }
        bluetoothStore.dispatch(BluetoothAction.ScanAndConnect)
    }

    override fun dispatch(action: DeviceAction) {
        // TODO: later on we'll switch between BTLE and WiFi here
    }

}