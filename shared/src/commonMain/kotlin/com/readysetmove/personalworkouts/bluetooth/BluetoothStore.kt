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
    val activeDevice: Device?,
) : State

sealed class BluetoothAction : Action {
    sealed class ScanAndConnect {
        companion object : BluetoothAction()
    }

    sealed class StopScanning {
        companion object : BluetoothAction()
    }

    data class DeviceConnected(val device: Device) : BluetoothAction()
    data class Error(val error: Exception) : BluetoothAction()
}


sealed class BluetoothSideEffect : Effect {
    data class Error(val error: Exception) : BluetoothSideEffect()
}

class BluetoothStore(private val bluetoothService: BluetoothService) :
    Store<BluetoothState, BluetoothAction, BluetoothSideEffect>,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(
        BluetoothState(scanning = false, activeDevice = null)
    )
    private val sideEffect = MutableSharedFlow<BluetoothSideEffect>()

    override fun observeState(): StateFlow<BluetoothState> = state
    override fun observeSideEffect(): Flow<BluetoothSideEffect> = sideEffect

    override fun dispatch(action: BluetoothAction) {
        val oldState = state.value

        val newState = when (action) {
            is BluetoothAction.ScanAndConnect.Companion -> {
                if (!oldState.scanning) {
                    launch { scanForBtleDevice() }
                    oldState.copy(scanning = true)
                } else {
                    oldState
                }
            }
            is BluetoothAction.StopScanning.Companion -> {
                if (oldState.scanning) {
                    bluetoothService.stopScan()
                    oldState.copy(scanning = false)
                } else {
                    oldState
                }
            }
            is BluetoothAction.DeviceConnected -> {
                if (oldState.activeDevice != action.device) {
                    oldState.copy(activeDevice = action.device)
                } else {
                    oldState
                }
            }
            is BluetoothAction.Error -> {
                if (oldState.scanning) {
                    launch { sideEffect.emit(BluetoothSideEffect.Error(action.error)) }
                    BluetoothState(scanning = false, activeDevice = null)
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

    private suspend fun scanForBtleDevice() {
        try {
            val foundDevice = bluetoothService.scanForDevice("Roberts Waage")
            // TODO: connect to device
            delay(1000)
            dispatch(BluetoothAction.DeviceConnected(foundDevice))
            dispatch(BluetoothAction.StopScanning)
        } catch (e: Exception) {
            dispatch(BluetoothAction.Error(e))
        }
    }
}