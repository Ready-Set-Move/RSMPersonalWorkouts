package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.MockTimestampProvider
import com.readysetmove.personalworkouts.TestStores
import com.readysetmove.personalworkouts.workout.progress.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class WorkoutStoreTest {

    @Test
    fun `the store yields the progress for the set workout`() = runTest {
        val stores = TestStores(testScheduler)
        val tractionGoal = 1337000L
        val setDuration = 2000L
        val setRestTime = 1000L
        val workout = WorkoutBuilder.workout {
            exercise {
                set(Set((tractionGoal/1000).toInt(), (setDuration/1000).toInt(), (setRestTime/1000).toInt()))
            }
        }
        val workoutProgress = WorkoutProgress(workout = workout)
        stores.useWorkoutStore(
            timestampProvider = MockTimestampProvider(timestamps = ArrayDeque(elements = listOf(
                // start of set
                0,
                // during 2000ms set work
                900,
                1800,
                // leading to set finish
                2100,
                // start of rest
                2200,
                // during 1000ms rest
                3000,
                // leading to rest finish
                3300,
            ))),
        ) {
            expect { WorkoutProgressState.NoWorkout }
                .let {
                    dispatch { it.startWorkoutAction(workout) }
                }
            expect { WorkoutProgressState.WaitingToStartExercise(workoutProgress = workoutProgress) }
                .let {
                    dispatch { it.startExerciseAction() }
                }
            val workingState = ExercisingState(
                workoutProgress = workoutProgress,
                timeLeft = setDuration,
                tractionGoal = tractionGoal,
                durationGoal = setDuration,
            )
            expect { WorkoutProgressState.WaitingToStartSet(exercisingState = workingState) }
                .let {
                    dispatch { it.startSetAction() }
                }

            // start working
            expect {
                WorkoutProgressState.Working(
                    startTime = 0,
                    exercisingState = workingState.copy(timeLeft = setDuration),
                )
            }
            expect {
                WorkoutProgressState.Working(
                    startTime = 0,
                    exercisingState = workingState.copy(timeLeft = 1100),
                )
            }
            expect {
                WorkoutProgressState.Working(
                    startTime = 0,
                    exercisingState = workingState.copy(timeLeft = 200),
                )
            }
            // stop working
            expect {
                WorkoutProgressState.Working(
                    startTime = 0,
                    exercisingState = workingState.copy(timeLeft = 0),
                )
            }
            // start rest
            val restingExercisingState = workingState.copy(
                timeLeft = setRestTime,
            )
            expect { WorkoutProgressState.Resting(startTime = 2200, exercisingState = restingExercisingState) }
            expect { WorkoutProgressState.Resting(startTime = 2200, exercisingState = restingExercisingState.copy(timeLeft = 200)) }
            expect { WorkoutProgressState.Resting(startTime = 2200, exercisingState = restingExercisingState.copy(timeLeft = 0)) }
            // stop resting and finish set
            expect { WorkoutProgressState.SetFinished(exercisingState = restingExercisingState.copy(timeLeft = 0)) }
        }
   }

    @Test
    fun `the store progresses through a complex workout`() = runTest {
        val stores = TestStores(testScheduler)
        val timestampProvider = MockTimestampProvider()
        val workout = WorkoutBuilder.workout {
            exercise("Rows") {
                set(Set(10, 15, 5))
                set(Set(40))
                set(Set(40))
            }
            exercise("DL") {
                set(Set(30, 15))
            }
        }
        val workoutStartProgress = WorkoutProgress(
            workout = workout,
        )
        stores.useWorkoutStore(
            timestampProvider = timestampProvider,
        ) {
            expect { WorkoutProgressState.NoWorkout }
                .let {
                    dispatch { it.startWorkoutAction(workout) }
                }
            var waitingToStartExerciseState: WorkoutProgressState.WaitingToStartExercise = expect { WorkoutProgressState.WaitingToStartExercise(workoutProgress = workoutStartProgress) }
            dispatch { waitingToStartExerciseState.startExerciseAction() }
            val ex1Set1Duration = 15000L
            var workingState = ExercisingState(
                workoutProgress = workoutStartProgress,
                timeLeft = ex1Set1Duration,
                tractionGoal = 10000,
                durationGoal = ex1Set1Duration,
            )
            val waitingState = expect { WorkoutProgressState.WaitingToStartSet(exercisingState = workingState) }
            // start working
            step("Runs the first set of the first exercise") {
                timestampProvider.timestamps.add(0)
                dispatch { waitingState.startSetAction() }
                /* start working */
                expect {
                    WorkoutProgressState.Working(
                        startTime = 0,
                        exercisingState = workingState,
                    )
                }
                // one timestamp 1s before set end
                timestampProvider.timestamps.add(14000)
                expect {
                    WorkoutProgressState.Working(
                        startTime = 0,
                        exercisingState = workingState.copy(timeLeft = 1000),
                    )
                }
                // one timestamp 1s after set end
                timestampProvider.timestamps.add(16000)
                expect {
                    WorkoutProgressState.Working(
                        startTime = 0,
                        exercisingState = workingState.copy(timeLeft = 0),
                    )
                }
                /* start rest */
                val restStartingTime = 20000L
                timestampProvider.timestamps.add(restStartingTime)
                val restingExercisingState = workingState.copy(
                    timeLeft = 5000,
                )
                expect { WorkoutProgressState.Resting(startTime = restStartingTime, exercisingState = restingExercisingState) }
                // one timestamp 1s before rest end
                timestampProvider.timestamps.add(24000)
                expect { WorkoutProgressState.Resting(startTime = restStartingTime, exercisingState = restingExercisingState.copy(timeLeft = 1000)) }
                // one timestamp 1s after rest end
                timestampProvider.timestamps.add(26000)
                expect { WorkoutProgressState.Resting(startTime = restStartingTime, exercisingState = restingExercisingState.copy(timeLeft = 0)) }
                // stop resting and finish set
                val finishedState = expect { WorkoutProgressState.SetFinished(exercisingState = restingExercisingState.copy(timeLeft = 0)) }

                step("Proceeds by rating the first set") {
                    dispatch { finishedState.goToNextSetAction() }
                }
            }
            step("Runs the second set of the first exercise") {
                val ex1Set2Duration = 6000L
                val ex1Set2Progress = workoutStartProgress.copy(activeSetIndex = 1)
                workingState = ExercisingState(
                    workoutProgress = ex1Set2Progress,
                    tractionGoal = 40000,
                    durationGoal = ex1Set2Duration,
                    timeLeft = ex1Set2Duration,
                )
                val workStartingTime = 30000L
                timestampProvider.timestamps.add(workStartingTime)
                expect {
                    WorkoutProgressState.WaitingToStartSet(
                        exercisingState = workingState
                    )
                }.let {
                    dispatch { it.startSetAction() }
                }
                /* start working */
                expect {
                    WorkoutProgressState.Working(
                        startTime = workStartingTime,
                        exercisingState = workingState,
                    )
                }
                // one timestamp 3s before set end
                timestampProvider.timestamps.add(33000)
                expect {
                    WorkoutProgressState.Working(
                        startTime = workStartingTime,
                        exercisingState = workingState.copy(timeLeft = 3000),
                    )
                }
                // one timestamp 2s after set end
                timestampProvider.timestamps.add(38000)
                expect {
                    WorkoutProgressState.Working(
                        startTime = workStartingTime,
                        exercisingState = workingState.copy(timeLeft = 0),
                    )
                }
                /* start rest */
                val restStartingTime = 51000L
                timestampProvider.timestamps.add(restStartingTime)
                val restingExercisingState = workingState.copy(
                    timeLeft = 30000,
                )
                // one timestamp 10s before rest end
                timestampProvider.timestamps.add(71000)
                expect { WorkoutProgressState.Resting(startTime = restStartingTime, exercisingState = restingExercisingState) }
                expect {
                    WorkoutProgressState.Resting(
                        startTime = restStartingTime,
                        exercisingState = restingExercisingState.copy(timeLeft = 10000),
                    )
                }
                // one timestamp 1s after rest end
                timestampProvider.timestamps.add(82000)
                expect {
                    WorkoutProgressState.Resting(
                        startTime = restStartingTime,
                        exercisingState = restingExercisingState.copy(timeLeft = 0),
                    )
                }
                // stop resting and finish set
                val finishedState = expect { WorkoutProgressState.SetFinished(exercisingState = restingExercisingState.copy(timeLeft = 0)) }

                step("Proceeds by rating the second set") {
                    dispatch { finishedState.goToNextSetAction() }
                }
            }
            step("Runs the third set of the first exercise") {
                val ex1Set3Duration = 6000L
                val ex1Set3Progress = workoutStartProgress.copy(activeSetIndex = 2)
                workingState = ExercisingState(
                    workoutProgress = ex1Set3Progress,
                    tractionGoal = 40000,
                    durationGoal = ex1Set3Duration,
                    timeLeft = ex1Set3Duration,
                )
                val workStartingTime = 100000L
                timestampProvider.timestamps.add(workStartingTime)
                expect {
                    WorkoutProgressState.WaitingToStartSet(
                        exercisingState = workingState
                    )
                }.let {
                    dispatch { it.startSetAction() }
                }

                /* start working */
                expect {
                    WorkoutProgressState.Working(
                        startTime = workStartingTime,
                        exercisingState = workingState,
                    )
                }
                // one timestamp 3s before set end
                timestampProvider.timestamps.add(103000)
                expect {
                    WorkoutProgressState.Working(
                        startTime = workStartingTime,
                        exercisingState = workingState.copy(timeLeft = 3000),
                    )
                }
                // one timestamp 2s after set end
                timestampProvider.timestamps.add(108000)
                expect {
                    WorkoutProgressState.Working(
                        startTime = workStartingTime,
                        exercisingState = workingState.copy(timeLeft = 0),
                    )
                }
                /* start rest */
                val restingExercisingState = workingState.copy(
                    timeLeft = 30000,
                )
                val restStartingTime = 111000L
                timestampProvider.timestamps.add(restStartingTime)
                expect { WorkoutProgressState.Resting(startTime = restStartingTime, exercisingState = restingExercisingState) }
                // one timestamp 10s before rest end
                timestampProvider.timestamps.add(131000)
                expect {
                    WorkoutProgressState.Resting(
                        startTime = restStartingTime,
                        exercisingState = restingExercisingState.copy(timeLeft = 10000),
                    )
                }
                // one timestamp 1s after rest end
                timestampProvider.timestamps.add(142000)
                expect {
                    WorkoutProgressState.Resting(
                        startTime = restStartingTime,
                        exercisingState = restingExercisingState.copy(timeLeft = 0),
                    )
                }
                // stop resting and finish set
                val finishedState = expect { WorkoutProgressState.SetFinished(exercisingState = restingExercisingState.copy(timeLeft = 0)) }

                step("Proceeds by rating the second set and the first exercise") {
                    dispatch { finishedState.goToNextSetAction() }
                    expect { WorkoutProgressState.ExerciseFinished(workoutProgress = ex1Set3Progress) }
                        .let {
                            dispatch { it.goToNextExerciseAction() }
                        }
                    waitingToStartExerciseState = expect {
                        WorkoutProgressState.WaitingToStartExercise(workoutProgress = workoutStartProgress.copy(activeExerciseIndex = 1))
                    }
                }
            }
            step("Runs the first set of the second exercise") {
                /** Exercise 2 */
                val ex2Duration = 15000L
                val ex2Progress = workoutStartProgress.copy(activeExerciseIndex = 1)
                dispatch { waitingToStartExerciseState.startExerciseAction() }
                /** Set start */
                workingState = ExercisingState(
                    workoutProgress = ex2Progress,
                    tractionGoal = 30000,
                    durationGoal = ex2Duration,
                    timeLeft = ex2Duration,
                )
                val workStartingTime = 200000L
                timestampProvider.timestamps.add(workStartingTime)
                expect {
                    WorkoutProgressState.WaitingToStartSet(
                        exercisingState = workingState
                    )
                }.let {
                    dispatch { it.startSetAction() }
                }
                /* start working */
                expect {
                    WorkoutProgressState.Working(
                        startTime = workStartingTime,
                        exercisingState = workingState,
                    )
                }
                // one timestamp 5s before set end
                timestampProvider.timestamps.add(210000)
                expect {
                    WorkoutProgressState.Working(
                        startTime = workStartingTime,
                        exercisingState = workingState.copy(timeLeft = 5000),
                    )
                }
                // one timestamp 5s after set end
                timestampProvider.timestamps.add(220000)
                expect {
                    WorkoutProgressState.Working(
                        startTime = workStartingTime,
                        exercisingState = workingState.copy(timeLeft = 0),
                    )
                }
                /* start rest */
                val restingExercisingState = workingState.copy(
                    timeLeft = 30000,
                )
                val restStartingTime = 230000L
                timestampProvider.timestamps.add(restStartingTime)
                expect { WorkoutProgressState.Resting(startTime = restStartingTime, exercisingState = restingExercisingState) }
                // one timestamp 20s before rest end
                timestampProvider.timestamps.add(240000)
                expect {
                    WorkoutProgressState.Resting(
                        startTime = restStartingTime,
                        exercisingState = restingExercisingState.copy(timeLeft = 20000),
                    )
                }
                // one timestamp 10s after rest end
                timestampProvider.timestamps.add(270000)
                expect {
                    WorkoutProgressState.Resting(
                        startTime = restStartingTime,
                        exercisingState = restingExercisingState.copy(timeLeft = 0),
                    )
                }
                // stop resting and finish set
                val finishedState = expect { WorkoutProgressState.SetFinished(exercisingState = restingExercisingState.copy(timeLeft = 0)) }

                step("Finishes the workout by rating the set and the second exercise") {
                    dispatch { finishedState.goToNextSetAction() }
                    expect { WorkoutProgressState.ExerciseFinished(workoutProgress = ex2Progress) }
                        .let {
                            dispatch { it.goToNextExerciseAction() }
                        }
                    expect { WorkoutProgressState.WorkoutFinished(workout) }
                }
            }
        }

        // TODO: test emitted side effects
   }
}