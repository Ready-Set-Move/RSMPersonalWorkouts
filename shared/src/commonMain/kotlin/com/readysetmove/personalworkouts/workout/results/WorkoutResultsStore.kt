package com.readysetmove.personalworkouts.workout.results

import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.workout.Exercise
import com.readysetmove.personalworkouts.workout.WorkoutProgress
import com.readysetmove.personalworkouts.workout.activeExercise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class WorkoutResultsState(val workoutResults: WorkoutResults? = null): State

class WorkoutResultsStore(
    private val workoutResultsRepository: IsWorkoutResultsRepository,
    mainDispatcher: CoroutineContext,
):
    CoroutineScope by CoroutineScope(mainDispatcher)
{
    private val state = MutableStateFlow(WorkoutResultsState())
    var pendingTractions: List<Traction> = emptyList()

    fun observeState() = state

    fun rateSet(
        tractionGoal: Long,
        durationGoal: Long,
        workoutProgress: WorkoutProgress,
        rating: Int,
    ) {
        val setResult = SetResult(
            tractionGoal = tractionGoal,
            durationGoal = durationGoal,
            tractions = pendingTractions,
            rating = rating,
        )
        pendingTractions = emptyList()

        val workoutResults = state.value.workoutResults.copyWithSetResults(
            setWithResult = workoutProgress.activeSetIndex to setResult,
            exerciseName = workoutProgress.activeExercise().name,
            workoutId = workoutProgress.workout.id,
        )
        state.value = state.value.copy(
            workoutResults = workoutResults
        )
        launch {
            workoutResultsRepository.storeResults(workoutResults)
        }
    }

    fun rateExercise(
        comment: String,
        rating: Int,
        exercise: Exercise,
    ) {
        launch {
            workoutResultsRepository.rateExercise(
                comment = comment,
                rating = rating,
                exercise = exercise.name,
            )
        }
    }
}