package com.readysetmove.personalworkouts.bluetooth

import com.readysetmove.personalworkouts.bluetooth.BluetoothService.BluetoothException.BluetoothDisabledException
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class BluetoothState(
    val bluetoothEnabled: Boolean,
    val scanning: Boolean,
    val activeDevice: DeviceManager?,
    val deviceName: String?,
) : State

sealed class BluetoothAction : Action {
    data class SetDeviceName(val name: String?) : BluetoothAction()
    data class SetBluetoothEnabled(val enabled: Boolean) : BluetoothAction()
    object ScanAndConnect : BluetoothAction()
    object StopScanning : BluetoothAction()
    data class DeviceConnected(val device: DeviceManager) : BluetoothAction()
    object DeviceDisConnected : BluetoothAction()
}


sealed class BluetoothSideEffect : Effect {
    data class Error(val error: Exception) : BluetoothSideEffect()
    data class DeviceDisConnected(val deviceName: String) : BluetoothSideEffect()
}

class BluetoothStore(private val bluetoothService: BluetoothService, initialState: BluetoothState) :
    Store<BluetoothState, BluetoothAction, BluetoothSideEffect>,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(
        initialState.copy()
    )
    private val sideEffect = MutableSharedFlow<BluetoothSideEffect>()
    private var connectJob: Job? = null

    override fun observeState(): StateFlow<BluetoothState> = state
    override fun observeSideEffect(): Flow<BluetoothSideEffect> = sideEffect

//    init {
//        launch {
//            observeState().collect {
//                val connectJobActive = connectJob?.isActive ?: false
//                if (it.scanning && !connectJobActive) {
//                    if (connectJobActive) connectJob?.cancel()
//
//                    val deviceName = state.value.deviceName
//                    if (deviceName != null) {
//                        connectJob = launch {
//                            scanForBtleDevice(deviceName)
//                        }
//                    } else {
//                        dispatch(BluetoothAction.StopScanning)
//                    }
//                } else if (!it.scanning && connectJob?.isActive == true) {
//                    connectJob?.cancel()
//                }
//            }
//        }
//    }

    override fun dispatch(action: BluetoothAction) {
        val oldState = state.value

        val newState = when (action) {
            is BluetoothAction.SetBluetoothEnabled -> {
                if (action.enabled != oldState.bluetoothEnabled) {
                    connectJob?.cancel()
                    oldState.copy(
                        bluetoothEnabled = action.enabled,
                        // scanning is either off due to disabled or gets turned off now
                        scanning = false,
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
                    !oldState.bluetoothEnabled -> {
                        oldState
                    }
                    oldState.activeDevice != null -> {
                        oldState
                    }
                    oldState.deviceName == null -> {
                        launch {
                            sideEffect.emit(BluetoothSideEffect.Error(Exception("Unexpected action: can't scan for device without name")))
                        }
                        oldState
                    }
                    !oldState.scanning -> {
                        connectJob = launch { scanForBtleDevice(oldState.deviceName) }
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
                if (oldState.activeDevice != action.device) {
                    oldState.copy(activeDevice = action.device, scanning = false)
                } else {
                    oldState
                }
            }
            is BluetoothAction.DeviceDisConnected -> {
                if (oldState.activeDevice != null) {
                    launch { sideEffect.emit(BluetoothSideEffect.DeviceDisConnected(oldState.activeDevice.deviceName)) }
                    oldState.copy(activeDevice = null)
                } else {
                    oldState
                }
            }
        }

        if (newState != oldState) {
            state.value = newState
        }
    }

    private suspend fun scanForBtleDevice(deviceName: String) {
        try {
            bluetoothService.connectToDevice(deviceName).collect {
                if (it != null) {
                    dispatch(BluetoothAction.DeviceConnected(it))
                } else {
                    dispatch(BluetoothAction.DeviceDisConnected)
                }
            }
        } catch (e: Exception) {
            dispatch(BluetoothAction.StopScanning)
            when (e) {
                is BluetoothDisabledException ->
                    dispatch(BluetoothAction.SetBluetoothEnabled(false))
                else -> sideEffect.emit(BluetoothSideEffect.Error(e))
            }
        }
    }
}