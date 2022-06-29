package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.state.State
import io.github.aakira.napier.Napier

object WorkoutStateFactory {
    fun workoutStateOfNewWorkout(workout: Workout, firstSet: Set) =
        WorkoutState(
            workoutProgress = WorkoutProgress(
                workout = workout,
            ),
            timeToWork = firstSet.duration,
            timeToRest = 0,
            tractionGoal = firstSet.tractionGoal,
            durationGoal = firstSet.duration,
        )
}

data class WorkoutState(
    val workoutProgress: WorkoutProgress? = null,
    val timeToWork: Long = 0,
    val timeToRest: Long = 0,
    val tractionGoal: Long? = null,
    val durationGoal: Long? = null,
    val working: Boolean = false,
    val startTime: Long = 0,
) : State

fun WorkoutState.forNextExercise(): WorkoutState {
    if (workoutProgress == null) throw WorkoutStoreExceptions.WorkoutNotSetException

    val nextExerciseIndex = workoutProgress.activeExerciseIndex + 1
    val (tractionGoal, duration) = workoutProgress.workout.exercises[nextExerciseIndex].sets.first()
    return copy(
        workoutProgress = workoutProgress.copy(
            activeExerciseIndex = nextExerciseIndex,
            activeSetIndex = 0,
        ),
        timeToRest = 0,
        timeToWork = duration,
        tractionGoal = tractionGoal,
        durationGoal = duration,
    )
}

fun WorkoutState.forNextSet(): WorkoutState? {
    if (workoutProgress == null) throw WorkoutStoreExceptions.WorkoutNotSetException
    // was this the last set of the exercise? If so finish the exercise...
    val activeExercise = workoutProgress.activeExercise()
    if (workoutProgress.activeSet() === activeExercise.sets.last()) {
        Napier.d("Last set of ${activeExercise.name} finished.")
        return null
    }
    // ...otherwise set active set to next set
    val newWorkoutProgress = workoutProgress.copy(
        activeSetIndex = workoutProgress.activeSetIndex + 1
    )
    val nextSet = newWorkoutProgress.activeSet()
    Napier.d("Set ${workoutProgress.activeSetIndex} of ${activeExercise.name} finished. Starting $nextSet.")
    return copy(
        workoutProgress = newWorkoutProgress,
        timeToRest = 0,
        timeToWork = nextSet.duration,
        tractionGoal = nextSet.tractionGoal,
        durationGoal = nextSet.duration
    )
}

fun WorkoutState.forSetStart(startTime: Long): Pair<WorkoutState, WorkoutProgress> {
    if (workoutProgress == null) throw WorkoutStoreExceptions.WorkoutNotSetException

    return copy(
        working = true,
        startTime = startTime
    ) to workoutProgress
}

fun WorkoutState.forRest(startTime: Long): Pair<WorkoutState, WorkoutProgress> {
    if (workoutProgress == null) throw WorkoutStoreExceptions.WorkoutNotSetException

    return copy(
        timeToRest = workoutProgress.activeSet().restTime,
        timeToWork = 0,
        startTime = startTime,
    ) to workoutProgress
}