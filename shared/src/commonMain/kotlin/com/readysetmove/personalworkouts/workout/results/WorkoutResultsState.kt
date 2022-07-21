package com.readysetmove.personalworkouts.workout.results

import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.workout.Exercise
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgress
import com.readysetmove.personalworkouts.workout.progress.activeExercise
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsState.WaitingForTractions
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsState.WaitingToRateSet

interface WillAcceptTractions

sealed class WorkoutResultsAction: Action {
    data class SetTractions(val waitingToRateSet: WorkoutResultsState.WaitingToRateSet): WorkoutResultsAction()
    data class RateSet(
        val waitingForTractions: WorkoutResultsState.WaitingForTractions,
    ): WorkoutResultsAction()
    data class RateExercise(
        val waitingForTractions: WorkoutResultsState.WaitingForTractions,
    ): WorkoutResultsAction()
}
sealed class WorkoutResultsState: State {
    object NoResults: WorkoutResultsState(), WillAcceptTractions
    data class WaitingForTractions(val workoutResults: WorkoutResults): WorkoutResultsState(), WillAcceptTractions
    data class WaitingToRateSet(val workoutResults: WorkoutResults? = null, val tractions: List<Traction>):
        WorkoutResultsState()
}


fun WillAcceptTractions.setTractionsAction(tractions: List<Traction>):
        WorkoutResultsAction.SetTractions
{
    return if (this is WaitingForTractions) {
        WorkoutResultsAction.SetTractions(
            waitingToRateSet = WaitingToRateSet(workoutResults = workoutResults, tractions = tractions)
        )
    } else {
        WorkoutResultsAction.SetTractions(
            waitingToRateSet = WaitingToRateSet(tractions = tractions)
        )
    }
}

fun WorkoutResultsState.WaitingToRateSet.rateSetAction(
    tractionGoal: Long,
    durationGoal: Long,
    workoutProgress: WorkoutProgress,
    rating: Int,
): WorkoutResultsAction.RateSet {
    val setResult = SetResult(
        tractionGoal = tractionGoal,
        durationGoal = durationGoal,
        tractions = tractions,
        rating = rating,
    )

    return WorkoutResultsAction.RateSet(
        waitingForTractions = WaitingForTractions(
            workoutResults = workoutResults.copyWithSetResults(
                setWithResult = workoutProgress.activeSetIndex to setResult,
                exerciseName = workoutProgress.activeExercise().name,
                workoutId = workoutProgress.workout.id,
            )
        )
    )
}

fun WorkoutResultsState.WaitingForTractions.rateExerciseAction(
    comment: String,
    rating: Int,
    exercise: Exercise,
): WorkoutResultsAction.RateExercise {
    // TODO: implement exercise rating in workout results
    return WorkoutResultsAction.RateExercise(
        waitingForTractions = WaitingForTractions(workoutResults = workoutResults)
    )
}