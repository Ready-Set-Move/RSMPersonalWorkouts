package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.MockTimestampProvider
import com.readysetmove.personalworkouts.TestStores
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class WorkoutStoreTest {

    @Test
    fun theStoreYieldsTheProgressForTheSetWorkout() = runTest {
        val stores = TestStores(testScheduler)
        val tractionGoal = EntityMocks.ONE_SET_WORKOUT.exercises[0].sets[0].tractionGoal
        val initialTimeToWork = EntityMocks.ONE_SET_WORKOUT.exercises[0].sets[0].duration
        val workoutStartState = WorkoutState(
            workoutProgress = WorkoutProgress(
                workout = EntityMocks.ONE_SET_WORKOUT,
                activeExerciseIndex = 0,
                activeSetIndex = 0,
            ),
            tractionGoal = tractionGoal,
            durationGoal = initialTimeToWork,
            timeToWork = initialTimeToWork,
        )
        // the duration is 100 + 20s rest + 20s buffer
        val timestamps = 0..initialTimeToWork+40 step 10
        stores.useWorkoutStore(
            timestampProvider = MockTimestampProvider(timestamps),
        ) {
            expect { WorkoutState() }
            dispatch { WorkoutAction.StartWorkout(EntityMocks.ONE_SET_WORKOUT) }
            expect { workoutStartState }
            dispatch { WorkoutAction.StartSet }
            for (timeToWork in initialTimeToWork downTo 0 step 10) {
                expect { workoutStartState.copy(timeToWork = timeToWork, working = true) }
            }
            // stop working
            val restingWorkout = workoutStartState.copy(timeToWork = 0)
            expect { restingWorkout }
            // start the rest, set has 20s rest
            expect { restingWorkout.copy(timeToRest = 20, startTime = 110) }
            expect { restingWorkout.copy(timeToRest = 10, startTime = 110) }
            expect { restingWorkout.copy(timeToRest = 0, startTime = 110) }
        }.run()
        // TODO: test emitted side effects and more complex workout
   }
}