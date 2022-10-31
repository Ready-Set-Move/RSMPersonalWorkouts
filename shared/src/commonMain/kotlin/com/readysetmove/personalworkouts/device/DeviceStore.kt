package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.device.DeviceAction.ScanAndConnect
import com.readysetmove.personalworkouts.device.DeviceAction.SetConnectionType
import com.readysetmove.personalworkouts.wifi.WifiService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val classLogTag = "DeviceStore"

data class AsyncJobs(
  var connect: Job? = null,
  var disconnect: Job? = null,
  var weight: Job? = null,
  var deviceData: Job? = null,
  val ioDispatcher: CoroutineContext,
) : CoroutineScope by CoroutineScope(ioDispatcher)

fun AsyncJobs.launchDataJobs(weightJob: Job, deviceDataJob: Job, disconnectJob: Job) {
  Napier.d(tag = "AsyncJobs.launchDataJobs") { "Launching async jobs after connection" }
  weight = weightJob
  deviceData = deviceDataJob
  disconnect = disconnectJob
  disconnectJob.invokeOnCompletion {
    Napier.d(tag = "AsyncJobs.launchDataJobs.invokeOnCompletion") { "Disconnect job completed" }
    weightJob.cancel("weight")
    deviceDataJob.cancel("deviceData")
  }
}

fun AsyncJobs.reset(): AsyncJobs {
  Napier.d(tag = "AsyncJobs.reset") { "Resetting async jobs" }
  connect.cancel("connect")
  disconnect.cancel("disconnect")
  weight.cancel("weight")
  deviceData.cancel("deviceData")
  return AsyncJobs(ioDispatcher = ioDispatcher)
}

private fun Job?.cancel(name: String) {
  if (this == null || isCancelled) return
  Napier.d(tag = "AsyncJobs.reset") { "Canceling: $name" }
  cancel()
}

class DeviceStore(
  initialState: DeviceState = DeviceState(),
  private val bluetoothService: BluetoothService,
  private val wifiService: WifiService,
  private val mainDispatcher: CoroutineContext,
  private val ioDispatcher: CoroutineContext,
) :
  IsDeviceStore,
  CoroutineScope by CoroutineScope(mainDispatcher) {

  private val state = MutableStateFlow(initialState)
  override fun observeState(): StateFlow<DeviceState> = state

  private val sideEffect = MutableSharedFlow<DeviceSideEffect>()
  override fun observeSideEffect(): Flow<DeviceSideEffect> = sideEffect

  private var asyncJobs = AsyncJobs(ioDispatcher = ioDispatcher)
  private var connectedDeviceService: DeviceService? = null

  override fun dispatch(action: DeviceAction) {
    val methodTag = "${classLogTag}.dispatch"
    when (action) {
      is SetConnectionType -> {
        if (state.value.connectionConfiguration == action.connectionConfiguration) return
        Napier.d(tag = methodTag) { "SetConnectionType to ${action.connectionConfiguration}" }

        state.value = state.value.copy(
          connectionConfiguration = action.connectionConfiguration,
        )
        resetConnection()
      }
      is ScanAndConnect ->
        state.value.connectionConfiguration?.let {
          if (state.value.connectionState != ConnectionState.DISCONNECTED) return

          Napier.d(tag = methodTag) { "ScanAndConnect" }
          state.value = state.value.copy(connectionState = ConnectionState.CONNECTING)

          connect(connectionConfiguration = it) {
            when (it) {
              is ConnectionConfiguration.BLEConnection -> {
                bluetoothService.connectToDevice(it)
              }
              is ConnectionConfiguration.WifiConnection -> {
                wifiService.connectToDevice(it)
              }
            }
          }
        } ?: run {
          launch {
            sideEffect.emit(DeviceSideEffect.ConnectionNotConfigured)
          }
        }
      is DeviceAction.Calibrate -> connectedDeviceService?.calibrate()
      is DeviceAction.ReadSettings -> connectedDeviceService?.readSettings()
      is DeviceAction.SetTara -> connectedDeviceService?.setTara()
    }
  }

  private fun resetConnection() {
    Napier.d(tag = classLogTag) { "resetConnection" }
    state.value = DeviceState(
      connectionConfiguration = state.value.connectionConfiguration
    )
    asyncJobs = asyncJobs.reset()
    connectedDeviceService = null
  }

  private fun deviceDisconnected(cause: IsDisconnectCause) {
    Napier.d(tag = classLogTag) { "deviceDisconnected" }
    launch {
      sideEffect.emit(DeviceSideEffect.DeviceDisconnected(
        configuration = state.value.deviceConfiguration,
        disconnectCause = cause,
      ))
    }
  }

  private fun deviceConnected(
    deviceChangeFlow: Flow<DeviceChange>,
    connectionConfiguration: ConnectionConfiguration,
  ) {
    Napier.d(tag = classLogTag) { "deviceConnected" }
    state.value = state.value.copy(
      connectionState = ConnectionState.CONNECTED,
    )
    asyncJobs.launchDataJobs(
      weightJob = launch(ioDispatcher) {
        deviceChangeFlow
          .filterIsInstance<DeviceChange.WeightChanged>()
          .collect {
            state.value = state.value.copy(traction = it.traction)
          }
      },
      deviceDataJob = launch(ioDispatcher) {
        deviceChangeFlow
          .filterIsInstance<DeviceChange.DeviceDataChanged>()
          .collect {
            state.value = state.value.copy(deviceConfiguration = it.deviceConfiguration)
          }
      },
      disconnectJob = launch(ioDispatcher) {
        deviceChangeFlow
          .filterIsInstance<DeviceChange.Disconnected>()
          .first {
            deviceDisconnected(it.cause)
            return@first true
          }
      }
    )
    connectedDeviceService = when (connectionConfiguration) {
      is ConnectionConfiguration.BLEConnection -> bluetoothService
      is ConnectionConfiguration.WifiConnection -> wifiService
    }
    Napier.d(tag = "$classLogTag.deviceConnected") { "dispatching ReadSettings" }
    dispatch(DeviceAction.ReadSettings)
  }

  private fun connect(
    connectionConfiguration: ConnectionConfiguration,
    flowFactory: () -> Flow<DeviceChange>,
  ) {
    val methodTag = "$classLogTag.connect"
    Napier.d(tag = methodTag) { "Start connection process" }
    try {
      val changeFlow = flowFactory().shareIn(this, SharingStarted.WhileSubscribed())
      launch(ioDispatcher) {
        // wait for connection success or failure
        Napier.d(tag = methodTag) { "Waiting for connection flow." }
        changeFlow.first { result ->
          if (result is DeviceChange.Connected) {
            Napier.d(tag = methodTag) { "Connection success" }
            // is this the correct connection attempt?
            if (connectionConfiguration == result.connectionConfiguration) {
              deviceConnected(changeFlow, connectionConfiguration)
            }
            return@first true
          }

          if (result is DeviceChange.Disconnected) {
            Napier.e(tag = methodTag) { "Device disconnected during connection attempt: ${result.cause}" }
            deviceDisconnected(result.cause)
            resetConnection()
            return@first true
          }

          Napier.w(tag = methodTag) { "Got result before connection finished: $result" }
          false
        }
      }
        .also {
          asyncJobs.connect = it
        }
        .invokeOnCompletion {
          Napier.d(tag = methodTag) { "Connect job finished" }
          asyncJobs.connect = null
        }
    } catch (e: Exception) {
      Napier.e(tag = methodTag) { "Connection failure: $e" }
      resetConnection()
      launch {
        sideEffect.emit(DeviceSideEffect.Error(e))
      }
    }
  }
}
