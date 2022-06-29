package com.readysetmove.personalworkouts.bluetooth

import com.readysetmove.personalworkouts.TestStores
import io.mockk.every
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test

class BluetoothStoreTest {

    @Test
    fun `the store goes to connected state receives weight updates and disconnects on exception`() = runTest {
        TestStores(testScheduler).useBluetoothStore {
            prepare {
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
            }

            expect { initialState }
            dispatch { BluetoothAction.ScanAndConnect }
            expect { initialState.copy(scanning = true) }
            expect { initialState.copy(activeDevice = deviceName) }
            expect { initialState.copy(activeDevice = deviceName, traction = 1f) }
            expect { initialState.copy(activeDevice = deviceName, traction = 3f) }
            expect { initialState.copy(activeDevice = null, traction = 3f) }
        }
    }

    @Test
    fun `the store aborts scanning on stop scanning`() = runTest {
        TestStores(testScheduler).useBluetoothStore {
            prepare {
                val flowMock = flow<BluetoothService.BluetoothDeviceActions> {
                }

                every {
                    serviceMock.connectToDevice(deviceName = deviceName, externalScope = any())
                } returns flowMock
            }

            expect {  initialState }
            dispatch { BluetoothAction.ScanAndConnect }
            expect {  initialState.copy(scanning = true) }
            dispatch { BluetoothAction.StopScanning }
            expect { initialState }
        }
    }

    @Test
    fun `the store calls set tara and disconnects on bluetooth disabled`() = runTest {
        TestStores(testScheduler).useBluetoothStore {
            expect { initialState }
            dispatch { BluetoothAction.ScanAndConnect }
            expect { initialState.copy(scanning = true) }
            expect { initialState.copy(activeDevice = deviceName) }
            dispatch { BluetoothAction.SetTara }
            verifyMock { serviceMock.setTara() }
            dispatch { BluetoothAction.SetBluetoothEnabled(false) }
            expect { initialState.copy(bluetoothEnabled = false) }
        }
    }
}