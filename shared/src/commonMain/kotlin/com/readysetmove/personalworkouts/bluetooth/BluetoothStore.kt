package com.readysetmove.personalworkouts.bluetooth

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class BluetoothState(
    val scanning: Boolean,
    val devices: List<Device>,
    val activeDevice: Device?,
) : State

sealed class BluetoothAction : Action {
    data class StartScanning(val filter: String) : BluetoothAction()
    sealed class StopScanning {
        companion object : BluetoothAction()
    }

    data class DevicesDiscovered(val devices: List<Device>) : BluetoothAction()
    data class UseDevice(val device: Device) : BluetoothAction()
    data class Error(val error: Exception) : BluetoothAction()
}


sealed class BluetoothSideEffect : Effect {
    data class Error(val error: Exception) : BluetoothSideEffect()
}

class BluetoothStore :
    Store<BluetoothState, BluetoothAction, BluetoothSideEffect>,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(
        BluetoothState(scanning = false, devices = emptyList(), activeDevice = null)
    )
    private val sideEffect = MutableSharedFlow<BluetoothSideEffect>()

    override fun observeState(): StateFlow<BluetoothState> = state
    override fun observeSideEffect(): Flow<BluetoothSideEffect> = sideEffect

    override fun dispatch(action: BluetoothAction) {
        val oldState = state.value

        val newState = when (action) {
            is BluetoothAction.StartScanning -> {
                if (!oldState.scanning) {
                    launch { scanForBtleDevices(action.filter) }
                    oldState.copy(scanning = true)
                } else {
                    oldState
                }
            }
            is BluetoothAction.StopScanning.Companion -> {
                if (oldState.scanning) {
                    launch { stopScanningForBtleDevices() }
                    oldState.copy(scanning = false)
                } else {
                    oldState
                }
            }
            is BluetoothAction.DevicesDiscovered -> {
                oldState.copy(devices = action.devices)
            }
            is BluetoothAction.UseDevice -> {
                if (oldState.activeDevice != action.device) {
                    launch { useDevice() }
                    oldState.copy(activeDevice = action.device)
                } else {
                    oldState
                }
            }
            is BluetoothAction.Error -> {
                if (oldState.scanning) {
                    launch { sideEffect.emit(BluetoothSideEffect.Error(action.error)) }
                    BluetoothState(scanning = false, devices = emptyList(), activeDevice = null)
                } else {
                    // only expects error during scanning atm.
                    launch { sideEffect.emit(BluetoothSideEffect.Error(Exception("Unexpected action"))) }
                    oldState
                }
            }
        }

        if (newState != oldState) {
            state.value = newState
        }
    }

    private suspend fun scanForBtleDevices(filter: String) {
        try {
            delay(2000)
            dispatch(BluetoothAction.DevicesDiscovered(listOf(Device(name = "Scanned 1"),
                Device(name = "Scanned 2"))))
        } catch (e: Exception) {
            dispatch(BluetoothAction.Error(e))
        }
    }

    private suspend fun stopScanningForBtleDevices() {
        try {
            delay(1000)
            // stop scanning process
        } catch (e: Exception) {
            dispatch(BluetoothAction.Error(e))
        }
    }

    private suspend fun useDevice() {
        try {
            delay(1000)
            // connect to device
        } catch (e: Exception) {
            dispatch(BluetoothAction.Error(e))
        }
    }
}