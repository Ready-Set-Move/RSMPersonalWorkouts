package com.readysetmove.personalworkouts.bluetooth

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class BluetoothState(
    val scanning: Boolean,
    val activeDevice: Device?,
    val deviceName: String?,
) : State

sealed class BluetoothAction : Action {
    data class SetDeviceName(val name: String?) : BluetoothAction()
    object ScanAndConnect : BluetoothAction()
    data class StopScanning(val error: Exception? = null) : BluetoothAction()
    data class DeviceConnected(val device: Device) : BluetoothAction()
}


sealed class BluetoothSideEffect : Effect {
    data class Error(val error: Exception) : BluetoothSideEffect()
}

class BluetoothStore(private val bluetoothService: BluetoothService) :
    Store<BluetoothState, BluetoothAction, BluetoothSideEffect>,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(
        BluetoothState(scanning = false, activeDevice = null, deviceName = "Roberts Waage")
    )
    private val sideEffect = MutableSharedFlow<BluetoothSideEffect>()
    private var connectJob: Job? = null

    override fun observeState(): StateFlow<BluetoothState> = state
    override fun observeSideEffect(): Flow<BluetoothSideEffect> = sideEffect

    init {
        launch {
            observeState().collect {
                val connectJobActive = connectJob?.isActive ?: false
                if (it.scanning && !connectJobActive) {
                    if (connectJobActive) connectJob?.cancel()

                    val deviceName = state.value.deviceName
                    if (deviceName != null) {
                        connectJob = launch {
                            scanForBtleDevice(deviceName)
                        }
                    } else {
                        dispatch(BluetoothAction.StopScanning())
                    }
                } else if (!it.scanning && connectJob?.isActive == true) {
                    connectJob?.cancel()
                }
            }
        }
    }

    override fun dispatch(action: BluetoothAction) {
        val oldState = state.value

        val newState = when (action) {
            is BluetoothAction.SetDeviceName -> {
                dispatch(BluetoothAction.StopScanning())
                oldState.copy(deviceName = action.name)
            }
            is BluetoothAction.ScanAndConnect -> {
                when {
                    oldState.deviceName == null -> {
                        launch {
                            sideEffect.emit(BluetoothSideEffect.Error(Exception("Unexpected action: can't scan for device without name")))
                        }
                        oldState
                    }
                    !oldState.scanning -> {
                        oldState.copy(scanning = true)
                    }
                    else -> oldState
                }
            }
            is BluetoothAction.StopScanning -> {
                if (oldState.scanning) {
                    if (action.error != null) {
                        launch { sideEffect.emit(BluetoothSideEffect.Error(action.error)) }
                    }
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
        }

        if (newState != oldState) {
            state.value = newState
        }
    }

    private suspend fun scanForBtleDevice(deviceName: String) {
        try {
            val connectFlow = bluetoothService.scanForDevice(deviceName)
            delay(1000)
            val device = connectFlow.single()
            dispatch(BluetoothAction.DeviceConnected(device))
        } catch (e: Exception) {
            dispatch(BluetoothAction.StopScanning(e))
        }
    }
}