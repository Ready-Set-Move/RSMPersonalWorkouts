package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.app.AppAction
import com.readysetmove.personalworkouts.workout.Set
import com.readysetmove.personalworkouts.workout.WorkoutBuilder
import com.readysetmove.personalworkouts.workout.WorkoutState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AppStoreTest {

    @Test
    fun theStoreCanBeSetupAndTriggerWorkoutStart() = runTest {
        val stores = TestStores(testScheduler)
        val tractionGoal = 1337000L
        val setDuration = 100L
        val setRestTime = 20L
        val workout = WorkoutBuilder.workout {
            exercise {
                set(Set(tractionGoal, setDuration, setRestTime))
            }
        }
        stores.useWorkoutStore { workoutStore ->
            stores.useBluetoothStore { bluetoothStore ->
                stores.useDeviceStore(
                    bluetoothStore = bluetoothStore,
                ) { deviceStore ->
                    stores.useAppStore(
                        deviceStore = deviceStore,
                        workoutStore = workoutStore
                    ) {
                        // TODO: wire with repo and ktor mock to yield correct Workout for testing
                        dispatch { AppAction.StartWorkout }
                    }
                }
            }
            expect { WorkoutState() }
        }
    }
}