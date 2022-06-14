package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.MockTimestampProvider
import com.readysetmove.personalworkouts.TestStores
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class WorkoutStoreTest {

    @Test
    fun theStoreYieldsTheProgressForTheSetWorkout() = runTest {
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
                activeExerciseIndex = 0,
                activeSetIndex = 0,
            ),
            tractionGoal = tractionGoal,
            durationGoal = setDuration,
            timeToWork = setDuration,
        )
        // the duration is 100 + 20s rest + 20s buffer
        val timestamps = 0..setDuration+40 step 10
        stores.useWorkoutStore(
            timestampProvider = MockTimestampProvider(timestamps),
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
        // TODO: test emitted side effects and more complex workout
   }
}