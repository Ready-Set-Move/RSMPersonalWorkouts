package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.workout.WorkoutAction.*
import com.readysetmove.personalworkouts.workout.WorkoutState.*

sealed class WorkoutAction: Action {
    data class StartWorkout(val workoutStarted: WaitingToStartExercise): WorkoutAction()
    data class StartExercise(val exerciseStarted: WaitingToStartSet): WorkoutAction()
    data class StartSet(val waitingToStartSet: WaitingToStartSet): WorkoutAction()
    data class FinishWork(val working: Working): WorkoutAction()
    data class FinishSet(val resting: Resting): WorkoutAction()
    data class RateSet(val rating: Int, val setFinished: SetFinished): WorkoutAction()
    data class RateExercise(val rating: Int, val exerciseFinished: ExerciseFinished): WorkoutAction()
    data class SetTractionGoal(val waitingToStartSetWithUpdatedTraction: WaitingToStartSet): WorkoutAction()
    data class SetDurationGoal(val waitingToStartSetWithUpdatedDuration: WaitingToStartSet): WorkoutAction()
}

fun NoWorkout.startWorkoutAction(workout: Workout): StartWorkout {
    return StartWorkout(this.startWorkout(workout = workout))
}

fun WaitingToStartExercise.startExerciseAction(): StartExercise {
    return StartExercise(exerciseStarted = this.startExercise())
}

fun WaitingToStartSet.setDurationGoalAction(durationGoal: Long): SetDurationGoal {
    return SetDurationGoal(waitingToStartSetWithUpdatedDuration = this.setDurationGoal(durationGoal))
}

fun WaitingToStartSet.setTractionGoalAction(tractionGoal: Long): SetTractionGoal {
    return SetTractionGoal(waitingToStartSetWithUpdatedTraction = this.setTractionGoal(tractionGoal))
}

fun WaitingToStartSet.startSetAction(): StartSet {
    return StartSet(waitingToStartSet = this)
}

fun Working.finishWorkAction(): FinishWork {
    return FinishWork(working = this)
}

fun Resting.finishSetAction(): FinishSet {
    return FinishSet(resting = this)
}

fun SetFinished.rateSetAction(rating: Int): RateSet {
    return RateSet(
        rating = rating,
        setFinished = this,
    )
}

fun ExerciseFinished.rateExerciseAction(rating: Int): RateExercise {
    return RateExercise(
        rating = rating,
        exerciseFinished = this,
    )
}