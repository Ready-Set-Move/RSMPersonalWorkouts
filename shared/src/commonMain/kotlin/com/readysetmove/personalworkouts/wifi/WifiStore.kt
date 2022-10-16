package com.readysetmove.personalworkouts.wifi

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.SimpleStore
import com.readysetmove.personalworkouts.state.State
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

data class WifiState(val deviceName: String? = null): State

sealed class WifiAction: Action {
    object ScanAndConnect: WifiAction()
}

private const val classLogTag = "WifiStore"

class WifiStore(
    private val wifiService: WifiService,
    val mainDispatcher: CoroutineContext,
    private val ioDispatcher: CoroutineContext,
):
    SimpleStore<WifiState, WifiAction>,
    CoroutineScope by CoroutineScope(mainDispatcher)
{
    private val state = MutableStateFlow(WifiState())
    override fun observeState(): StateFlow<WifiState> = state

    override fun dispatch(action: WifiAction) {
        launch {
            connectToWifiDevice("rsm", this)
        }
    }

    private suspend fun connectToWifiDevice(deviceName: String, coroutineScope: CoroutineScope) =
        withContext(ioDispatcher) {
            val methodTag = "${classLogTag}.startConnectCallback"
            wifiService.connectToDevice(deviceName, coroutineScope).collect { action ->
                when(action) {
                    is WifiService.WifiDeviceActions.Connected ->
                        Napier.d(tag = methodTag) { "Connected" }
                    is WifiService.WifiDeviceActions.DisConnected ->
                        Napier.d(tag = methodTag) { "Not connected: ${action.cause}" }
                }
            }
        }

}