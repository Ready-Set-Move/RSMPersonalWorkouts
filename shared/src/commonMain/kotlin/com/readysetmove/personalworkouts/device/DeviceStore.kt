package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.device.DeviceAction.ScanAndConnect
import com.readysetmove.personalworkouts.device.DeviceAction.SetConnectionType
import com.readysetmove.personalworkouts.wifi.WifiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private const val classLogTag = "DeviceStore"

data class AsyncJobs(
    var connect: Job? = null,
    var disconnect: Job? = null,
    var weight: Job? = null,
    var deviceData: Job? = null,
    val ioDispatcher: CoroutineContext,
) : CoroutineScope by CoroutineScope(ioDispatcher)

fun AsyncJobs.launchConnect(block: suspend AsyncJobs.() -> Unit) {
    val zeeConnectJob = launch {
        block()
    }
    zeeConnectJob.invokeOnCompletion {
        connect = null
    }
    connect = zeeConnectJob
}
fun AsyncJobs.reset(): AsyncJobs {
    connect?.cancel()
    disconnect?.cancel()
    weight?.cancel()
    deviceData?.cancel()
    return AsyncJobs(ioDispatcher = ioDispatcher)
}

// TODO: add management of different devices
class DeviceStore(
    private val initialState: DeviceState = DeviceState(),
    private val bluetoothService: BluetoothService,
    private val wifiService: WifiService,
    private val mainDispatcher: CoroutineContext,
    private val ioDispatcher: CoroutineContext,
):
    IsDeviceStore,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(initialState)
    override fun observeState(): StateFlow<DeviceState> = state

    private val sideEffect = MutableSharedFlow<DeviceSideEffect>()
    override fun observeSideEffect(): Flow<DeviceSideEffect> = sideEffect

    private var asyncJobs = AsyncJobs(ioDispatcher = ioDispatcher)

    override fun dispatch(action: DeviceAction) {
        val methodTag = "${classLogTag}.dispatch"
        when(action) {
            is SetConnectionType -> {
                if (state.value.connectionConfiguration == action.connectionConfiguration) return

                state.value = state.value.copy(
                    connectionConfiguration = action.connectionConfiguration,
                )
                dispatch(DeviceAction.ResetConnection)
            }
            is ScanAndConnect ->
                state.value.connectionConfiguration?.apply {
                    dispatch(DeviceAction.ResetConnection)

                    state.value = state.value.copy(connectionState = ConnectionState.CONNECTING)

                    asyncJobs.launchConnect {
                        val connectionConfiguration = this@apply
                        connect(connectionConfiguration = connectionConfiguration) {
                        when(connectionConfiguration) {
                            is ConnectionConfiguration.BLEConnection -> {
                                bluetoothService.connectToDevice(
                                    connectionConfiguration,
                                    this
                                )
                            }
                            is ConnectionConfiguration.WifiConnection -> {
                                wifiService.connectToDevice(
                                    connectionConfiguration,
                                    this
                                )
                            }
                        }
                    }
                }
            } ?: run {
                launch {
                    sideEffect.emit(DeviceSideEffect.ConnectionNotConfigured)
                }
            }
            is DeviceAction.ResetConnection -> {
                state.value = DeviceState(
                    connectionConfiguration = state.value.connectionConfiguration
                )
                asyncJobs = asyncJobs.reset()
            }
        }
    }

    private suspend fun deviceDisconnected(cause: IsDisconnectCause) =
        withContext(mainDispatcher) {
            dispatch(DeviceAction.ResetConnection)
            launch {
                sideEffect.emit(DeviceSideEffect.DeviceDisconnected(
                    configuration = state.value.deviceConfiguration,
                    disconnectCause = cause,
                ))
            }
        }

    private suspend fun deviceConnected(deviceChangeFlow: Flow<DeviceChange>) =
        withContext(ioDispatcher) {
            state.value = state.value.copy(
                connectionState = ConnectionState.CONNECTED,
            )
            val weightJob = launch(ioDispatcher) {
                deviceChangeFlow
                    .filterIsInstance<DeviceChange.WeightChanged>()
                    .collect {
                        state.value = state.value.copy(traction = it.traction)
                    }
            }
            val deviceDataJob = launch(ioDispatcher) {
                deviceChangeFlow
                    .filterIsInstance<DeviceChange.DeviceDataChanged>()
                    .collect {
                        state.value = state.value.copy(deviceConfiguration = it.deviceConfiguration)
                    }
            }
            val disconnectJob = launch(ioDispatcher) {
                deviceChangeFlow
                    .filterIsInstance<DeviceChange.Disconnected>()
                    .first {
                        deviceDisconnected(it.cause)
                        return@first true
                    }
            }
            asyncJobs = asyncJobs.copy(
                weight = weightJob,
                deviceData = deviceDataJob,
                disconnect = disconnectJob
            )
        }

    private suspend fun connect(
        connectionConfiguration: ConnectionConfiguration,
        flowFactory: () -> Flow<DeviceChange>
    ) =
        withContext(ioDispatcher) {
            try {
                val changeFlow = flowFactory()
                // wait for connection success or failure
                // TODO: test if connect job finishes automatically after this block
                changeFlow.first { result ->
                    if (result is DeviceChange.Connected) {
                        // is this the correct connection attempt?
                        if (connectionConfiguration == result.connectionConfiguration) {
                            deviceConnected(changeFlow)
                        }
                        return@first true
                    }

                    if (result is DeviceChange.Disconnected) {
                        deviceDisconnected(result.cause)
                        return@first true
                    }

                    false
                }
            } catch (e: Exception) {
                dispatch(DeviceAction.ResetConnection)
                sideEffect.emit(DeviceSideEffect.Error(e))
            }
        }
}
