package com.readysetmove.personalworkouts.bluetooth

import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.BluetoothConnectPermissionNotGrantedException
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.BluetoothDisabledException
import com.readysetmove.personalworkouts.device.DeviceConfiguration
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

data class BluetoothState(
    val bluetoothEnabled: Boolean,
    val bluetoothPermissionsGranted: Boolean = false,
    val scanning: Boolean = false,
    val activeDevice: String? = null,
    val deviceName: String? = null,
    val traction: Float = 0.0f,
    val deviceConfiguration: DeviceConfiguration? = null
) : State

sealed class BluetoothAction : Action {
    data class SetDeviceName(val name: String?) : BluetoothAction()
    data class SetBluetoothEnabled(val enabled: Boolean) : BluetoothAction()
    data class SetBluetoothPermissionsGranted(val granted: Boolean) : BluetoothAction()
    object ScanAndConnect : BluetoothAction()
    object StopScanning : BluetoothAction()
    object SetTara : BluetoothAction()
    object ReadSettings : BluetoothAction()
    object Calibrate : BluetoothAction()
}


sealed class BluetoothSideEffect : Effect {
    data class Error(val error: Exception) : BluetoothSideEffect()
    data class FatalError(val error: Exception) : BluetoothSideEffect()
    data class DeviceDisConnected(val deviceName: String) : BluetoothSideEffect()
}

class BluetoothStore(
    private val bluetoothService: BluetoothService,
    initialState: BluetoothState,
    val mainDispatcher: CoroutineContext,
    val ioDispatcher: CoroutineContext,
) :
    Store<BluetoothState, BluetoothAction, BluetoothSideEffect>,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val classLogTag = "BluetoothStore"
    private val state = MutableStateFlow(
        initialState.copy()
    )
    private val sideEffect = MutableSharedFlow<BluetoothSideEffect>()
    private var connectJob: Job? = null

    override fun observeState(): StateFlow<BluetoothState> = state
    override fun observeSideEffect(): Flow<BluetoothSideEffect> = sideEffect

    override fun dispatch(action: BluetoothAction) {
        when (action) {
            is BluetoothAction.SetBluetoothEnabled -> {
                if (action.enabled != state.value.bluetoothEnabled) {
                    connectJob?.cancel()
                    state.value = state.value.copy(
                        bluetoothEnabled = action.enabled,
                        activeDevice = null,
                    )
                }
            }
            is BluetoothAction.SetBluetoothPermissionsGranted -> {
                if (action.granted != state.value.bluetoothPermissionsGranted) {
                    connectJob?.cancel()
                    state.value = state.value.copy(
                        bluetoothPermissionsGranted = action.granted,
                        activeDevice = null,
                    )
                }
            }
            is BluetoothAction.SetDeviceName -> {
                if (state.value.deviceName != action.name) {
                    connectJob?.cancel()
                    state.value = state.value.copy(deviceName = action.name)
                }
            }
            is BluetoothAction.ScanAndConnect -> {
                val deviceName = state.value.deviceName
                val btEnabled = state.value.bluetoothEnabled
                val permissionsGranted = state.value.bluetoothPermissionsGranted
                Napier.d(tag = classLogTag) {
                    "ScanAndConnect device: $deviceName. With enabled=$btEnabled | permissions=$permissionsGranted"
                }
                when {
                    // TODO: why is nothing logged here?
                    !btEnabled -> {
                        // TODO: side effect to inform consumer
                        Napier.d(tag = classLogTag) { "Bluetooth not enabled." }
                        return
                    }
                    !permissionsGranted -> {
                        // TODO: side effect to inform consumer
                        Napier.d(tag = classLogTag) { "Bluetooth permissions missing." }
                        return
                    }
                    state.value.activeDevice != null || connectJob != null -> {
                        Napier.d(tag = classLogTag) { "Connection already open while attempting to reconnect" }
                        // in this case we already started a connection
                        return
                    }
                    deviceName == null -> {
                        launch {
                            sideEffect.emit(BluetoothSideEffect.Error(Exception("Unexpected action: can't scan for device without name")))
                        }
                        return
                    }
                    !state.value.scanning -> {
                        state.value = state.value.copy(scanning = true)
                        val zeeConnectJob = launch {
                            scanForBtleDevice(
                                deviceName = deviceName,
                                coroutineScope = this)
                        }
                        zeeConnectJob.invokeOnCompletion {
                            if (state.value.scanning) state.value = state.value.copy(scanning = false)
                            connectJob = null
                        }
                        connectJob = zeeConnectJob
                    }
                }
            }
            is BluetoothAction.StopScanning -> {
                if (state.value.scanning) {
                    connectJob?.cancel()
                    state.value = state.value.copy(scanning = false)
                }
            }
            is BluetoothAction.SetTara -> {
                if (state.value.activeDevice != null) bluetoothService.setTara()
            }
            is BluetoothAction.ReadSettings -> {
                if (state.value.activeDevice != null) bluetoothService.readSettings()
            }
            is BluetoothAction.Calibrate -> {
                if (state.value.activeDevice != null) bluetoothService.calibrate()
            }
        }
    }

    private suspend fun scanForBtleDevice(deviceName: String, coroutineScope: CoroutineScope) =
        withContext(ioDispatcher) {
            try {
                bluetoothService.connectToDevice(deviceName, coroutineScope).collect { action ->
                    when (action) {
                        is BluetoothService.BluetoothDeviceActions.Connected ->
                            if (state.value.activeDevice != action.deviceName) {
                                state.value = state.value.copy(
                                    activeDevice = action.deviceName,
                                    scanning = false
                                )
                            }
                        is BluetoothService.BluetoothDeviceActions.WeightChanged ->
                            state.value = state.value.copy(traction = action.traction)
                        is BluetoothService.BluetoothDeviceActions.DeviceDataChanged ->
                            state.value = state.value.copy(deviceConfiguration = action.deviceConfiguration)
                        is BluetoothService.BluetoothDeviceActions.DisConnected -> {
                            state.value.activeDevice?.let {
                                state.value = state.value.copy(activeDevice = null)
                                sideEffect.emit(BluetoothSideEffect.DeviceDisConnected(it))
                            }
                            when (action.cause) {
                                is BluetoothDisabledException ->
                                    dispatch(BluetoothAction.SetBluetoothEnabled(false))
                                is BluetoothConnectPermissionNotGrantedException ->
                                    dispatch(BluetoothAction.SetBluetoothPermissionsGranted(false))
                                is BluetoothService.BluetoothException.ConnectionBrokenException -> {
                                    Napier.d(tag = classLogTag) { action.cause.toString() }
                                    // TODO: need to catch these and restart the App
                                    sideEffect.emit(BluetoothSideEffect.FatalError(action.cause))
                                }
                                else -> sideEffect.emit(BluetoothSideEffect.Error(action.cause))
                            }
                            cancel()
                        }
                    }
                }
            } catch (e: Exception) {
                dispatch(BluetoothAction.StopScanning)
                sideEffect.emit(BluetoothSideEffect.Error(e))
            }
        }
}