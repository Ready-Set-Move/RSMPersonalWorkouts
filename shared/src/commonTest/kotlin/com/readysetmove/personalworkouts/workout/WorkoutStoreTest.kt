package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.MockTimestampProvider
import com.readysetmove.personalworkouts.TestStores
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class WorkoutStoreTest {

    @Test
    fun `the store yields the progress for the set workout`() = runTest {
        val stores = TestStores(testScheduler)
        val tractionGoal = 1337000L
        val setDuration = 100L
        val setRestTime = 20L
        val workout = WorkoutBuilder.workout {
            exercise {
                set(Set(tractionGoal, setDuration, setRestTime))
            }
        }
        val workoutStartState = WorkoutState(
            workoutProgress = WorkoutProgress(
                workout = workout,
            ),
            tractionGoal = tractionGoal,
            durationGoal = setDuration,
            timeToWork = setDuration,
        )
        stores.useWorkoutStore(
            // the duration is 100 + 20s rest + 20s buffer
            timestampProvider = MockTimestampProvider(timestamps = ArrayDeque(elements = (0..setDuration+40 step 10).toList())),
        ) {
            expect { WorkoutState() }
            dispatch { WorkoutAction.StartWorkout(workout) }
            expect { workoutStartState }
            dispatch { WorkoutAction.StartSet }
            for (timeToWork in setDuration downTo 0 step 10) {
                expect { workoutStartState.copy(timeToWork = timeToWork, working = true) }
            }
            // stop working
            val restingWorkout = workoutStartState.copy(timeToWork = 0)
            expect { restingWorkout }
            // start the rest, set has 20s rest
            expect { restingWorkout.copy(timeToRest = setRestTime, startTime = 110) }
            expect { restingWorkout.copy(timeToRest = setRestTime-10, startTime = 110) }
            expect { restingWorkout.copy(timeToRest = 0, startTime = 110) }
        }
   }

    @Test
    fun `the store progresses through a complex workout`() = runTest {
        val stores = TestStores(testScheduler)
        val timestampProvider = MockTimestampProvider()
        val workout = WorkoutBuilder.workout {
            exercise("Rows") {
                set(Set(10, 15000, 5000))
                set(Set(40))
                set(Set(40))
            }
            exercise("DL") {
                set(Set(30, 15000))
            }
        }
        val workoutStartProgress = WorkoutProgress(
            workout = workout,
        )
        stores.useWorkoutStore(
            timestampProvider = timestampProvider,
        ) {
            expect { WorkoutState() }
            dispatch { WorkoutAction.StartWorkout(workout) }
            timestampProvider.timestamps.add(0)
            var currentState = expect { WorkoutState(
                workoutProgress = workoutStartProgress,
                tractionGoal = 10,
                durationGoal = 15000,
                timeToWork = 15000,
            ) }
            step("Runs the first set of the first exercise") {
                dispatch { WorkoutAction.StartSet }
                /* start working */
                currentState = expect { currentState.copy(
                    working = true,
                ) }
                // one timestamp 1s before set end
                timestampProvider.timestamps.add(14000)
                currentState = expect { currentState.copy(
                    timeToWork = 1000,
                ) }
                // one timestamp 1s after set end
                timestampProvider.timestamps.add(16000)
                currentState = expect { currentState.copy(
                    timeToWork = 0,
                ) }
                currentState = expect { currentState.copy(
                    working = false,
                ) }
                /* start rest */
                timestampProvider.timestamps.add(20000)
                currentState = expect { currentState.copy(
                    startTime = 20000,
                    timeToRest = 5000,
                ) }
                // one timestamp 1s before rest end
                timestampProvider.timestamps.add(24000)
                currentState = expect { currentState.copy(
                    timeToRest = 1000,
                ) }
                // one timestamp 1s after rest end
                timestampProvider.timestamps.add(26000)
                currentState = expect { currentState.copy(
                    timeToRest = 0,
                ) }

                currentState = expect { currentState.copy(
                    workoutProgress = workoutStartProgress.copy(activeSetIndex = 1),
                    timeToWork = 6000,
                    tractionGoal = 40,
                    durationGoal = 6000,
                ) }
            }
            step("Runs the second set of the first exercise") {
                timestampProvider.timestamps.add(30000)
                dispatch { WorkoutAction.StartSet }
                /* start working */
                currentState = expect { currentState.copy(
                    working = true,
                    startTime = 30000,
                ) }
                // one timestamp 3s before set end
                timestampProvider.timestamps.add(33000)
                currentState = expect { currentState.copy(
                    timeToWork = 3000,
                ) }
                // one timestamp 2s after set end
                timestampProvider.timestamps.add(38000)
                currentState = expect { currentState.copy(
                    timeToWork = 0,
                ) }
                currentState = expect { currentState.copy(
                    working = false,
                ) }
                /* start rest */
                timestampProvider.timestamps.add(51000)
                currentState = expect { currentState.copy(
                    startTime = 51000,
                    timeToRest = 30000,
                ) }
                // one timestamp 10s before rest end
                timestampProvider.timestamps.add(71000)
                currentState = expect { currentState.copy(
                    timeToRest = 10000,
                ) }
                // one timestamp 1s after rest end
                timestampProvider.timestamps.add(82000)
                currentState = expect { currentState.copy(
                    timeToRest = 0,
                ) }

                currentState = expect { currentState.copy(
                    workoutProgress = workoutStartProgress.copy(
                        activeSetIndex = 2,
                    ),
                    durationGoal = 6000,
                    tractionGoal = 40,
                    timeToWork = 6000,
                ) }
            }
            step("Runs the third set of the first exercise") {
                timestampProvider.timestamps.add(40000)
                dispatch { WorkoutAction.StartSet }
                /* start working */
                currentState = expect { currentState.copy(
                    working = true,
                    startTime = 40000,
                ) }
                // one timestamp 3s before set end
                timestampProvider.timestamps.add(43000)
                currentState = expect { currentState.copy(
                    timeToWork = 3000,
                ) }
                // one timestamp 2s after set end
                timestampProvider.timestamps.add(48000)
                currentState = expect { currentState.copy(
                    timeToWork = 0,
                ) }
                currentState = expect { currentState.copy(
                    working = false,
                ) }
                /* start rest */
                timestampProvider.timestamps.add(51000)
                currentState = expect { currentState.copy(
                    startTime = 51000,
                    timeToRest = 30000,
                ) }
                // one timestamp 10s before rest end
                timestampProvider.timestamps.add(71000)
                currentState = expect { currentState.copy(
                    timeToRest = 10000,
                ) }
                // one timestamp 1s after rest end
                timestampProvider.timestamps.add(82000)
                currentState = expect { currentState.copy(
                    timeToRest = 0,
                ) }

                currentState = expect { currentState.copy(
                    workoutProgress = workoutStartProgress.copy(
                        activeExerciseIndex = 1,
                        activeSetIndex = 0,
                    ),
                    durationGoal = 15000,
                    tractionGoal = 30,
                    timeToWork = 15000,
                ) }
            }
            step("Runs the first set of the second exercise") {
                /** Exercise 2 */
                /** Set start */
                timestampProvider.timestamps.add(100000)
                dispatch { WorkoutAction.StartSet }
                /* start working */
                currentState = expect { currentState.copy(
                    working = true,
                    startTime = 100000,
                ) }
                // one timestamp 5s before set end
                timestampProvider.timestamps.add(110000)
                currentState = expect { currentState.copy(
                    timeToWork = 5000,
                ) }
                // one timestamp 5s after set end
                timestampProvider.timestamps.add(120000)
                currentState = expect { currentState.copy(
                    timeToWork = 0,
                ) }
                currentState = expect { currentState.copy(
                    working = false,
                ) }
                /* start rest */
                timestampProvider.timestamps.add(130000)
                currentState = expect { currentState.copy(
                    startTime = 130000,
                    timeToRest = 30000,
                ) }
                // one timestamp 20s before rest end
                timestampProvider.timestamps.add(140000)
                currentState = expect { currentState.copy(
                    timeToRest = 20000,
                ) }
                // one timestamp 10s after rest end
                timestampProvider.timestamps.add(170000)
                currentState = expect { currentState.copy(
                    timeToRest = 0,
                ) }
            }
        }

        // TODO: test emitted side effects and more complex workout
   }
}