package com.readysetmove.personalworkouts.wifi

import com.readysetmove.personalworkouts.device.DeviceConfiguration
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.SimpleStore
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.wifi.WifiService.WifiConnectionType
import com.readysetmove.personalworkouts.wifi.WifiService.WifiDeviceActions.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

data class WifiState(
    val connection: WifiConnectionType? = null,
    val wifiEnabled: Boolean,
    val wifiPermissionsGranted: Boolean = false,
    val connecting: Boolean = false,
    val host: String? = null,
    val traction: Float = 0.0f,
    val deviceConfiguration: DeviceConfiguration? = null
): State

sealed class WifiAction: Action {
    data class SetConnectionType(val connection: WifiConnectionType?): WifiAction()
    object ScanAndConnect: WifiAction()
}

private const val classLogTag = "WifiStore"

class WifiStore(
    initialState: WifiState,
    private val wifiService: WifiService,
    val mainDispatcher: CoroutineContext,
    private val ioDispatcher: CoroutineContext,
):
    SimpleStore<WifiState, WifiAction>,
    CoroutineScope by CoroutineScope(mainDispatcher)
{
    private val state = MutableStateFlow(initialState)
    override fun observeState(): StateFlow<WifiState> = state
    private var connectJob: Job? = null

    override fun dispatch(action: WifiAction) {
        val methodTag = "${classLogTag}.dispatch"
        when(action) {
            is WifiAction.SetConnectionType -> {
                if (state.value.connection == action.connection) return

                state.value = state.value.copy(connection = action.connection)
                connectJob?.cancel()

            }
            is WifiAction.ScanAndConnect -> {
                val connection = state.value.connection
                val enabled = state.value.wifiEnabled
                val permissionsGranted = state.value.wifiPermissionsGranted
                Napier.d(tag = methodTag) {
                    "ScanAndConnect: $connection. With enabled=$enabled | permissions=$permissionsGranted"
                }

                when {
                    !enabled -> {
                        // TODO: side effect to inform consumer
                        Napier.d(tag = methodTag) {
                            "wifi not enabled"
                        }
                    }
                    !permissionsGranted -> {
                        // TODO: side effect to inform consumer
                        Napier.d(tag = methodTag) {
                            "wifi permissions not granted"
                        }
                    }
                    connection == null -> {
                        // TODO: side effect to inform consumer
                        Napier.d(tag = methodTag) {
                            "Connection type not set"
                        }
                    }
                    !state.value.connecting -> {
                        state.value = state.value.copy(connecting = true)
                        val zeeConnectJob = launch {
                            connectToWifiDevice(connection, this)
                        }
                        zeeConnectJob.invokeOnCompletion {
                            if (state.value.connecting) state.value = state.value.copy(connecting = false)
                            connectJob = null
                        }
                        connectJob = zeeConnectJob
                    }
                }
            }
        }
    }

    private suspend fun connectToWifiDevice(wifiConnectionType: WifiConnectionType, coroutineScope: CoroutineScope) =
        withContext(ioDispatcher) {
            val methodTag = "${classLogTag}.connectToWifiDevice"
            wifiService.connectToDevice(wifiConnectionType, coroutineScope).collect { action ->
                when(action) {
                    is Connected ->
                        state.value = state.value.copy(host = action.connectedHost)
                    is DisConnected ->
                        Napier.d(tag = methodTag) { "Not connected: ${action.cause}" }
                    is WeightChanged ->
                        state.value = state.value.copy(traction = action.traction)
                    is DeviceDataChanged ->
                        state.value = state.value.copy(deviceConfiguration = action.deviceConfiguration)
                }
            }
        }

}