package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.app.AppState
import com.readysetmove.personalworkouts.app.AppStore
import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothState
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import com.readysetmove.personalworkouts.device.DeviceStore
import com.readysetmove.personalworkouts.workout.WorkoutState
import com.readysetmove.personalworkouts.workout.WorkoutStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.yield

data class TestStores(val testScheduler: TestCoroutineScheduler)
    : CoroutineScope by CoroutineScope(UnconfinedTestDispatcher(testScheduler)) {

    fun useAppStore(
        bluetoothStore: BluetoothStore,
        workoutStore: WorkoutStore,
        prepareTest: () -> Unit = {},
        verify: ((List<AppState>) -> Unit)?,
        runTest: (AppStore) -> Unit,
    ) {
        val deviceStore = DeviceStore(
            mainDispatcher = this.coroutineContext,
            bluetoothStore = bluetoothStore,
        )

        val appStore = AppStore(
            workoutStore = workoutStore,
            deviceStore = deviceStore,
            mainDispatcher = this.coroutineContext,
        )
        val values = mutableListOf<AppState>()
        val gatherStatesJob = if (verify != null) launch {
            appStore.observeState().toList(values)
        } else null
        prepareTest()
        runTest(appStore)
        testScheduler.advanceUntilIdle()
        gatherStatesJob?.cancel()
        if (verify != null) verify(values)
    }

    fun useWorkoutStore(
        prepareTest: () -> Unit = {},
        verify: ((List<WorkoutState>) -> Unit)?,
        runTest: (WorkoutStore) -> Unit,
    ) {
        val workoutStore = WorkoutStore(
            mainDispatcher = this.coroutineContext,
        )
        val values = mutableListOf<WorkoutState>()
        val gatherStatesJob = if (verify != null) launch {
            workoutStore.observeState().toList(values)
        } else null
        prepareTest()
        runTest(workoutStore)
        testScheduler.advanceUntilIdle()
        gatherStatesJob?.cancel()
        if (verify != null) verify(values)
    }

    /**
     * @param prepareTest Default behaviour sets up connection and simple setTara Mock
     * @param verify is optional as more complex tests may not want to verify bt states
     */
    fun useBluetoothStore(
        prepareTest: ((BluetoothService, String) -> Unit) = { serviceMock, deviceName ->
            val flowMock = flow<BluetoothService.BluetoothDeviceActions> {
                emit(BluetoothService.BluetoothDeviceActions.Connected(deviceName))
                yield()
            }

            every {
                serviceMock.connectToDevice(deviceName = deviceName, externalScope = any())
            } returns flowMock
            every {
                serviceMock.setTara()
            } returns Unit
        },
        verify: ((List<BluetoothState>, BluetoothState, String, BluetoothService) -> Unit)? = null,
        runTest: (BluetoothStore) -> Unit,
    ) {
        val deviceName = "ZeeDevice"
        val serviceMock: BluetoothService = mockk()
        val initialState = BluetoothState(
            bluetoothEnabled = true,
            deviceName = deviceName,
            bluetoothPermissionsGranted = true,
        )
        val bluetoothStore = BluetoothStore(
            bluetoothService = serviceMock,
            initialState = initialState,
            mainDispatcher = this.coroutineContext,
            ioDispatcher = this.coroutineContext,
        )
        val values = mutableListOf<BluetoothState>()
        val gatherBluetoothStatesJob = if (verify != null) launch {
            bluetoothStore.observeState().toList(values)
        } else null
        prepareTest(serviceMock, deviceName)
        runTest(bluetoothStore)
        gatherBluetoothStatesJob?.cancel()
        if (verify != null) verify(values, initialState, deviceName, serviceMock)
    }
}

