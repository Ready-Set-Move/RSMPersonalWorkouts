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
    val weight: Float = 0.0f,
) : State

sealed class BluetoothAction : Action {
    data class SetDeviceName(val name: String?) : BluetoothAction()
    data class SetBluetoothEnabled(val enabled: Boolean) : BluetoothAction()
    data class SetBluetoothPermissionsGranted(val granted: Boolean) : BluetoothAction()
    object ScanAndConnect : BluetoothAction()
    object StopScanning : BluetoothAction()
    data class DeviceConnected(val deviceName: String) : BluetoothAction()

    //    object DeviceDisConnected : BluetoothAction()
    object SetTara : BluetoothAction()
}


sealed class BluetoothSideEffect : Effect {
    data class Error(val error: Exception) : BluetoothSideEffect()
    data class DeviceDisConnected(val deviceName: String) : BluetoothSideEffect()
}

class BluetoothStore(
    private val bluetoothService: BluetoothService,
    initialState: BluetoothState,
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

        val newState = when (action) {
            is BluetoothAction.SetBluetoothEnabled -> {
                if (action.enabled != oldState.bluetoothEnabled) {
                    connectJob?.cancel()
                    connectJob = null
                    oldState.copy(
                        bluetoothEnabled = action.enabled,
                        // scanning is either off due to disabled or gets turned off now
                        scanning = false,
                        activeDevice = null,
                    )
                } else {
                    oldState
                }
            }
            is BluetoothAction.SetBluetoothPermissionsGranted -> {
                if (action.granted != oldState.bluetoothPermissionsGranted) {
                    connectJob?.cancel()
                    connectJob = null
                    oldState.copy(
                        bluetoothPermissionsGranted = action.granted,
                        // scanning is either off due to no permissions or gets turned off now
                        scanning = false,
                        activeDevice = null,
                    )
                } else {
                    oldState
                }
            }
            is BluetoothAction.SetDeviceName -> {
                if (oldState.deviceName != action.name) {
                    connectJob?.cancel()
                    oldState.copy(deviceName = action.name)
                } else {
                    oldState
                }
            }
            is BluetoothAction.ScanAndConnect -> {
                when {
                    !oldState.bluetoothEnabled || !oldState.bluetoothPermissionsGranted -> {
                        // TODO: side effect to inform consumer
                        oldState
                    }
                    oldState.activeDevice != null || connectJob != null -> {
                        // in this case we already started a connection
                        oldState
                    }
                    oldState.deviceName == null -> {
                        launch {
                            sideEffect.emit(BluetoothSideEffect.Error(Exception("Unexpected action: can't scan for device without name")))
                        }
                        oldState
                    }
                    !oldState.scanning -> {
                        val zeeConnectJob = launch {
                            scanForBtleDevice(
                                deviceName = oldState.deviceName,
                                coroutineScope = this)
                        }
                        zeeConnectJob.invokeOnCompletion {
                            connectJob = null
                        }
                        connectJob = zeeConnectJob
                        oldState.copy(scanning = true)
                    }
                    else -> oldState
                }
            }
            is BluetoothAction.StopScanning -> {
                if (oldState.scanning) {
                    connectJob?.cancel()
                    oldState.copy(scanning = false)
                } else {
                    oldState
                }
            }
            is BluetoothAction.DeviceConnected -> {
                if (oldState.activeDevice != action.deviceName) {
                    oldState.copy(activeDevice = action.deviceName, scanning = false)
                } else {
                    oldState
                }
            }
            is BluetoothAction.SetTara -> {
                bluetoothService.setTara()
                oldState
            }
        }

        if (newState != oldState) {
            state.value = newState
        }
    }

    private suspend fun scanForBtleDevice(deviceName: String, coroutineScope: CoroutineScope) =
        withContext(ioDispatcher) {
            try {
                bluetoothService.connectToDevice(deviceName, coroutineScope).collect { action ->
                    when (action) {
                        is BluetoothService.BluetoothDeviceActions.Connected -> dispatch(
                            BluetoothAction.DeviceConnected(
                                action.deviceName))
                        is BluetoothService.BluetoothDeviceActions.WeightChanged -> state.value =
                            state.value.copy(weight = action.weight)
                        is BluetoothService.BluetoothDeviceActions.DisConnected -> {
                            state.value.activeDevice?.let {
                                sideEffect.emit(BluetoothSideEffect.DeviceDisConnected(it))
                                state.value = state.value.copy(activeDevice = null)
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