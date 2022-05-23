package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.TestStores
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutStoreTest {

    @Test
    fun theStoreYieldsTheProgressForTheSetWorkout() = runTest {
        val stores = TestStores(testScheduler)
        stores.useBluetoothStore {
            bluetoothStore ->
            stores.useWorkoutStore(bluetoothStore = bluetoothStore,
                verify = { workoutStates ->
                    val tractionGoal = EntityMocks.ONE_SET_WORKOUT.exercises[0].sets[0].tractionGoal
                    val initialTimeToWork = EntityMocks.ONE_SET_WORKOUT.exercises[0].sets[0].duration
                    val expectedStates = mutableListOf<WorkoutState>()
                    expectedStates.add(WorkoutState())
                    expectedStates.add(WorkoutState(
                        workoutProgress =  WorkoutProgress(
                            workout = EntityMocks.ONE_SET_WORKOUT,
                            activeExercise = EntityMocks.ONE_SET_WORKOUT.exercises[0],
                            activeSet = EntityMocks.ONE_SET_WORKOUT.exercises[0].sets[0]
                        ),
                        tractionGoal = tractionGoal,
                        timeToWork = initialTimeToWork
                    ))
                    for (timeToWork in initialTimeToWork downTo 0 step 10) {
                        expectedStates.add(
                            WorkoutState(
                                workoutProgress =  WorkoutProgress(
                                    workout = EntityMocks.ONE_SET_WORKOUT,
                                    activeExercise = EntityMocks.ONE_SET_WORKOUT.exercises[0],
                                    activeSet = EntityMocks.ONE_SET_WORKOUT.exercises[0].sets[0]
                                ),
                                tractionGoal = tractionGoal,
                                timeToWork = timeToWork,
                                working = true
                            )
                        )
                    }
                    expectedStates.add(WorkoutState(
                        workoutProgress =  WorkoutProgress(
                            workout = EntityMocks.ONE_SET_WORKOUT,
                            activeExercise = EntityMocks.ONE_SET_WORKOUT.exercises[0],
                            activeSet = EntityMocks.ONE_SET_WORKOUT.exercises[0].sets[0]
                        ),
                        tractionGoal = tractionGoal,
                        timeToWork = initialTimeToWork,
                    ))
                    assertEquals(
                        expectedStates,
                        workoutStates,
                        "Check app flow"
                    )
                }
            ) { workoutStore ->
                workoutStore.dispatch(WorkoutAction.StartWorkout(EntityMocks.ONE_SET_WORKOUT))
                workoutStore.dispatch(WorkoutAction.StartSet)
                advanceUntilIdle()
            }
        }
    }
}