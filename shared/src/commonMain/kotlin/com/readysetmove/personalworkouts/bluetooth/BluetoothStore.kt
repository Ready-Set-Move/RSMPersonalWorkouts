package com.readysetmove.personalworkouts.bluetooth

import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.BluetoothConnectPermissionNotGrantedException
import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.BluetoothDisabledException
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
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
) : State

sealed class BluetoothAction : Action {
    data class SetDeviceName(val name: String?) : BluetoothAction()
    data class SetBluetoothEnabled(val enabled: Boolean) : BluetoothAction()
    data class SetBluetoothPermissionsGranted(val granted: Boolean) : BluetoothAction()
    object ScanAndConnect : BluetoothAction()
    object StopScanning : BluetoothAction()
    object SetTara : BluetoothAction()
}


sealed class BluetoothSideEffect : Effect {
    data class Error(val error: Exception) : BluetoothSideEffect()
    data class DeviceDisConnected(val deviceName: String) : BluetoothSideEffect()
}

class BluetoothStore(
    private val bluetoothService: BluetoothService,
    initialState: BluetoothState,
    val mainDispatcher: CoroutineContext,
    val ioDispatcher: CoroutineContext,
) :
    Store<BluetoothState, BluetoothAction, BluetoothSideEffect>,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(
        initialState.copy()
    )
    private val sideEffect = MutableSharedFlow<BluetoothSideEffect>()
    private var connectJob: Job? = null

    override fun observeState(): StateFlow<BluetoothState> = state
    override fun observeSideEffect(): Flow<BluetoothSideEffect> = sideEffect

    override fun dispatch(action: BluetoothAction) {
        val oldState = state.value

        when (action) {
            is BluetoothAction.SetBluetoothEnabled -> {
                if (action.enabled != oldState.bluetoothEnabled) {
                    connectJob?.cancel()
                    connectJob = null
                    state.value = oldState.copy(
                        bluetoothEnabled = action.enabled,
                        // scanning is either off due to disabled or gets turned off now
                        scanning = false,
                        activeDevice = null,
                    )
                }
            }
            is BluetoothAction.SetBluetoothPermissionsGranted -> {
                if (action.granted != oldState.bluetoothPermissionsGranted) {
                    connectJob?.cancel()
                    connectJob = null
                    state.value = oldState.copy(
                        bluetoothPermissionsGranted = action.granted,
                        // scanning is either off due to no permissions or gets turned off now
                        scanning = false,
                        activeDevice = null,
                    )
                }
            }
            is BluetoothAction.SetDeviceName -> {
                if (oldState.deviceName != action.name) {
                    connectJob?.cancel()
                    state.value = oldState.copy(deviceName = action.name)
                }
            }
            is BluetoothAction.ScanAndConnect -> {
                when {
                    !oldState.bluetoothEnabled || !oldState.bluetoothPermissionsGranted -> {
                        // TODO: side effect to inform consumer
                        return
                    }
                    oldState.activeDevice != null || connectJob != null -> {
                        // in this case we already started a connection
                        return
                    }
                    oldState.deviceName == null -> {
                        launch(mainDispatcher) {
                            sideEffect.emit(BluetoothSideEffect.Error(Exception("Unexpected action: can't scan for device without name")))
                        }
                        return
                    }
                    !oldState.scanning -> {
                        state.value = oldState.copy(scanning = true)
                        val zeeConnectJob = launch(ioDispatcher) {
                            scanForBtleDevice(
                                deviceName = oldState.deviceName,
                                coroutineScope = this)
                        }
                        zeeConnectJob.invokeOnCompletion {
                            connectJob = null
                        }
                        connectJob = zeeConnectJob
                    }
                }
            }
//            is BluetoothAction.DeviceConnected -> {
//                if (oldState.activeDevice != action.deviceName) {
//                    oldState.copy(
//                        activeDevice = action.deviceName,
//                        scanning = false
//                    )
//                }
//                else oldState
//            }
//            is BluetoothAction.DeviceDisconnected -> {
//                oldState
//            }
            is BluetoothAction.StopScanning -> {
                if (oldState.scanning) {
                    connectJob?.cancel()
                    state.value = oldState.copy(scanning = false)
                }
            }
            is BluetoothAction.SetTara -> {
                bluetoothService.setTara()
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