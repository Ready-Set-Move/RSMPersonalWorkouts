package com.readysetmove.personalworkouts.bluetooth

import com.readysetmove.personalworkouts.TestStores
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

class BluetoothStoreTest {

    @Test
    fun theStoreGoesToConnectedStateReceivesWeightUpdatesAndDisconnectsOnException() = runTest {
        TestStores(testScheduler).useBluetoothStore(
            prepareTest = { serviceMock, deviceName ->
                val flowMock = flow {
                    emit(BluetoothService.BluetoothDeviceActions.Connected(deviceName))
                    yield()
                    emit(BluetoothService.BluetoothDeviceActions.WeightChanged(1f))
                    yield()
                    emit(BluetoothService.BluetoothDeviceActions.WeightChanged(3f))
                    yield()
                    emit(BluetoothService.BluetoothDeviceActions.DisConnected(
                        BluetoothService.BluetoothException.NotConnectedException("Device shut down")))
                }
                every {
                    serviceMock.connectToDevice(deviceName = deviceName, externalScope = any())
                } returns flowMock
            },
            verify = { bluetoothStates, initialState, deviceName, serviceMock ->
                verify { serviceMock.connectToDevice(deviceName = deviceName, externalScope = any()) }
                assertEquals(
                    listOf(
                        initialState,
                        initialState.copy(scanning = true),
                        initialState.copy(activeDevice = deviceName),
                        initialState.copy(activeDevice = deviceName, traction = 1f),
                        initialState.copy(activeDevice = deviceName, traction = 3f),
                        initialState.copy(activeDevice = null, traction = 3f),
                    ),
                    bluetoothStates,
                    "Check app flow"
                )
            },
        ) { bluetoothStore ->
            bluetoothStore.dispatch(BluetoothAction.ScanAndConnect)
        }
    }

    @Test
    fun theStoreAbortsScanningOnStopScanning() = runTest {
        TestStores(testScheduler).useBluetoothStore(
            prepareTest = { serviceMock, deviceName ->
                val flowMock = flow<BluetoothService.BluetoothDeviceActions> {
                }

                every {
                    serviceMock.connectToDevice(deviceName = deviceName, externalScope = any())
                } returns flowMock
            },
            verify = { values, initialState, _, _ ->
                assertEquals(
                    listOf(
                        initialState,
                        initialState.copy(scanning = true),
                        initialState,
                    ),
                    values,
                    "Check stop scanning")
            },
        ) { bluetoothStore ->
            bluetoothStore.dispatch(BluetoothAction.ScanAndConnect)
            bluetoothStore.dispatch(BluetoothAction.StopScanning)
        }
    }

    @Test
    fun theStoreCallsSetTaraAndDisconnectsOnBluetoothDisabled() = runTest {
        TestStores(testScheduler).useBluetoothStore(
            verify = { values, initialState, deviceName, serviceMock ->
                verify { serviceMock.setTara() }
                assertEquals(
                    listOf(
                        initialState,
                        initialState.copy(scanning = true),
                        initialState.copy(activeDevice = deviceName),
                        initialState.copy(bluetoothEnabled = false),
                    ),
                    values,
                    "Check stop scanning")
            }
        ) { bluetoothStore ->
            bluetoothStore.dispatch(BluetoothAction.ScanAndConnect)
            bluetoothStore.dispatch(BluetoothAction.SetTara)
            bluetoothStore.dispatch(BluetoothAction.SetBluetoothEnabled(false))
        }
    }
}