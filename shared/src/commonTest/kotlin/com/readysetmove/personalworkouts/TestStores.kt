package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.app.AppAction
import com.readysetmove.personalworkouts.app.AppSideEffect
import com.readysetmove.personalworkouts.app.AppState
import com.readysetmove.personalworkouts.app.AppStore
import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothState
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import com.readysetmove.personalworkouts.device.*
import com.readysetmove.personalworkouts.workout.*
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.yield

data class TestStores(val testScheduler: TestCoroutineScheduler)
    : CoroutineScope by CoroutineScope(UnconfinedTestDispatcher(testScheduler)) {

    fun useAppStore(
        deviceStore: IsDeviceStore = MockDeviceStore(mainDispatcher = this.coroutineContext),
        workoutStore: WorkoutStore,
        init: StoreTester<AppState, AppAction, AppSideEffect>.() -> Unit,
    ) {
        val appStore = AppStore(
            workoutStore = workoutStore,
            deviceStore = deviceStore,
            mainDispatcher = this.coroutineContext,
            workoutRepository = object: IsWorkoutRepository {
                override suspend fun fetchLatestWorkoutForUser(userId: String): Workout {
                    return WorkoutBuilder.workout {
                        exercise {
                            assessmentTest(20, 30, 40)
                        }
                    }
                }

                override suspend fun saveWorkout(userId: String, workout: Workout) {
                    TODO("Not yet implemented")
                }
            },
            workoutResultsRepository = WorkoutResultsRepository(),
        )
        val storeTester = StoreTester(
            store = appStore,
            testScheduler = testScheduler,
        )
        storeTester.init()
        storeTester.run()
    }

    fun useDeviceStore(
        timestampProvider: IsTimestampProvider = MockTimestampProvider(),
        bluetoothStore: BluetoothStore,
        init: StoreTester<DeviceState, DeviceAction, DeviceSideEffect>.(deviceStore: IsDeviceStore) -> Unit,
    ) {
        val deviceStore = DeviceStore(
            mainDispatcher = this.coroutineContext,
            bluetoothStore = bluetoothStore,
            timestampProvider = timestampProvider,
        )
        val storeTester = StoreTester(
            store = deviceStore,
            testScheduler = testScheduler,
        )
        storeTester.init(deviceStore)
        storeTester.run()
    }

    fun useWorkoutStore(
        timestampProvider: IsTimestampProvider = MockTimestampProvider(),
        init: StoreTester<WorkoutState, WorkoutAction, WorkoutSideEffect>.(workoutStore: WorkoutStore) -> Unit,
    ) {
        val workoutStore = WorkoutStore(
            mainDispatcher = this.coroutineContext,
            timestampProvider = timestampProvider
        )
        val storeTester = StoreTester(
            store = workoutStore,
            testScheduler = testScheduler,
        )
        storeTester.init(workoutStore)
        storeTester.run()
    }

    fun useBluetoothStore(
        init: BluetoothStoreTester.(bluetoothStore: BluetoothStore) -> Unit,
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
        val storeTester = StoreTester(
            store = bluetoothStore,
            testScheduler = testScheduler,
        )
        val bluetoothStoreTester = BluetoothStoreTester(
            tester = storeTester,
            deviceName = deviceName,
            serviceMock = serviceMock,
            initialState = initialState,
        )
        bluetoothStoreTester.prepare {
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
        }
        bluetoothStoreTester.init(bluetoothStore)
        storeTester.run()
    }
}

