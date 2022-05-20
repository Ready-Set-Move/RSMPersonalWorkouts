package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.bluetooth.BluetoothAction
import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothState
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

class BluetoothStoreTest {

    @Test
    fun testStoreGoesToConnectedStateReceivesWeightUpdatesAndDisconnectsOnException() = runTest {
        val deviceName = "ZeeDevice"
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
        val serviceMock = mockk<BluetoothService>()
        every {
            serviceMock.connectToDevice(deviceName = deviceName, externalScope = any())
        } returns flowMock
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val initialState = BluetoothState(
            bluetoothEnabled = true,
            deviceName = deviceName,
            bluetoothPermissionsGranted = true,
        )
        val store = BluetoothStore(
            bluetoothService = serviceMock,
            initialState = initialState,
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )
        val values = mutableListOf<BluetoothState>()
        val stateGatherJob = launch(dispatcher) {
            store.observeState().toList(values)
        }
        store.dispatch(BluetoothAction.ScanAndConnect)
        stateGatherJob.cancel()
        verify { serviceMock.connectToDevice(deviceName = deviceName, externalScope = any()) }
        assertEquals(listOf(
                initialState,
                initialState.copy(scanning = true),
                initialState.copy(activeDevice = deviceName),
                initialState.copy(activeDevice = deviceName, traction = 1f),
                initialState.copy(activeDevice = deviceName, traction = 3f),
                initialState.copy(activeDevice = null, traction = 3f),
            ),
            values,
            "Check app flow")
    }
}