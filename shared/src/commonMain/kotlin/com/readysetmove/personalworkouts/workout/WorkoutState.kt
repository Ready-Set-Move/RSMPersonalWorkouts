package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.state.State

object WorkoutStateFactory {
    fun workoutStateOfNewWorkout(workout: Workout, firstSet: Set) =
        WorkoutState(
            workoutProgress = WorkoutProgress(
                workout = workout,
            ),
            timeToWork = firstSet.duration*1000L,
            timeToRest = 0,
            tractionGoal = firstSet.tractionGoal*1000L,
            durationGoal = firstSet.duration*1000L,
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

fun WorkoutState.forWorkoutProgress(workoutProgress: WorkoutProgress): WorkoutState {
    val currentSet = workoutProgress.activeSet()
    val durationInMs = currentSet.duration*1000L
    return copy(
        workoutProgress = workoutProgress,
        timeToRest = 0,
        timeToWork = durationInMs,
        tractionGoal = currentSet.tractionGoal*1000L,
        durationGoal = durationInMs,
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
        timeToRest = workoutProgress.activeSet().restTime*1000L,
        timeToWork = 0,
        startTime = startTime,
    ) to workoutProgress
}