package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.app.AppAction
import com.readysetmove.personalworkouts.workout.WorkoutState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AppStoreTest {

    @Test
    fun `the store can be setup and trigger workout start`() = runTest {
        val stores = TestStores(testScheduler)
        stores.useWorkoutStore { workoutStore ->
            stores.useBluetoothStore { bluetoothStore ->
                stores.useDeviceStore(
                    bluetoothStore = bluetoothStore,
                ) { deviceStore ->
                    stores.useAppStore(
                        deviceStore = deviceStore,
                        workoutStore = workoutStore
                    ) {
                        dispatch { AppAction.StartWorkout }
                    }
                }
            }
            expect { WorkoutState.NoWorkout }
        }
    }
}