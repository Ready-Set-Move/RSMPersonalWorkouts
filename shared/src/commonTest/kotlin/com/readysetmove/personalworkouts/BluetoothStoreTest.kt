package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.bluetooth.BluetoothAction
import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothState
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BluetoothStoreTest {

    @Test
    fun testStoreGoesToConnectedState() = runTest {
        val deviceName = "ZeeDevice"
        val flowMock = flow {
            emit(BluetoothService.BluetoothDeviceActions.Connected(deviceName))
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
        val job = launch(dispatcher) {
            store.observeState().collect {
                values.add(it)
            }
        }
        store.dispatch(BluetoothAction.ScanAndConnect)
        job.cancel()
        verify { serviceMock.connectToDevice(deviceName = deviceName, externalScope = any()) }
        assertEquals(listOf(
                initialState,
                initialState.copy(scanning = true),
                initialState.copy(activeDevice = deviceName)
            ),
            values,
            "Check device is set")
    }
}