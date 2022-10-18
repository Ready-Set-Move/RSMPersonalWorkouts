package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import com.readysetmove.personalworkouts.wifi.WifiAction
import com.readysetmove.personalworkouts.wifi.WifiStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val classLogTag = "DeviceStore"

// TODO: add management of different devices
class DeviceStore(
    private val bluetoothStore: BluetoothStore,
    private val wifiStore: WifiStore,
    private val mainDispatcher: CoroutineContext,
):
    IsDeviceStore,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(DeviceState())

    override fun observeState(): StateFlow<DeviceState> = state

    init {
        val methodTag = "$classLogTag.init"
        launch {
            bluetoothStore.observeState().collect { bluetoothState ->
                if (bluetoothState.activeDevice == null) return@collect
                state.value = state.value.copy(traction = bluetoothState.traction, deviceConfiguration = bluetoothState.deviceConfiguration)
            }
        }
        launch {
            wifiStore.observeState().collect { wifiState ->
                if (wifiState.host == null) return@collect
                state.value = state.value.copy(traction = wifiState.traction, deviceConfiguration = wifiState.deviceConfiguration)
            }
        }
//        bluetoothStore.dispatch(BluetoothAction.ScanAndConnect)
        wifiStore.dispatch(WifiAction.ScanAndConnect)
    }

    override fun dispatch(action: DeviceAction) {
        // TODO: later on we'll switch between BTLE and WiFi here
    }

}