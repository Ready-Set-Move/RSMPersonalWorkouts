package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.workout.WorkoutState.*
import com.readysetmove.personalworkouts.workout.WorkoutStateExceptions.*

interface IsExercisingState {
    val workoutProgress: WorkoutProgress
    val tractionGoal: Long
    val timeLeft: Long
}

data class ExercisingState(
    override val workoutProgress: WorkoutProgress,
    override val tractionGoal: Long,
    override val timeLeft: Long,
): IsExercisingState

sealed class WorkoutState : State {
    object NoWorkout : WorkoutState()
    data class WaitingToStartExercise(
        val workoutProgress: WorkoutProgress,
    ) : WorkoutState()
    data class WaitingToStartSet(
        val durationGoal: Long,
        val exercisingState: ExercisingState,
    ): WorkoutState(), IsExercisingState by exercisingState
    data class Working(
        val startTime: Long,
        val durationGoal: Long,
        val exercisingState: ExercisingState,
    ) : WorkoutState(), IsExercisingState by exercisingState
    data class Resting(
        val startTime: Long,
        val exercisingState: ExercisingState,
    ) : WorkoutState(), IsExercisingState by exercisingState
    data class SetFinished(
        val workoutProgress: WorkoutProgress,
    ) : WorkoutState()
    data class ExerciseFinished(
        val workoutProgress: WorkoutProgress,
    ) : WorkoutState()
    object WorkoutFinished : WorkoutState()
}

sealed class WorkoutStateExceptions : Exception() {
    object WorkoutNotSetException: WorkoutStateExceptions()
    object StartingExerciseFromWrongStateException: WorkoutStateExceptions()
    object StartingSetFromWrongStateException: WorkoutStateExceptions()
    object StartingRestFromWrongStateException: WorkoutStateExceptions()
    object FinishingSetFromNoExercisingState: WorkoutStateExceptions()
}

fun WaitingToStartSet.setDurationGoal(durationGoal: Long): WaitingToStartSet {
    return copy(
        durationGoal = durationGoal
    )
}

fun WaitingToStartSet.setTractionGoal(tractionGoal: Long): WaitingToStartSet {
    return copy(
        exercisingState = exercisingState.copy(tractionGoal = tractionGoal)
    )
}

fun WorkoutState.startWorkout(workout: Workout): Result<WaitingToStartExercise>  {
    if (this !is NoWorkout) return Result.failure(WorkoutNotSetException)

    return Result.success(WaitingToStartExercise(
        workoutProgress = WorkoutProgress(
            workout = workout,
        ),
    ))
}

fun WorkoutState.startSet(startTime: Long): Result<Working> {
    if (this !is WaitingToStartSet) return Result.failure(StartingSetFromWrongStateException)

    return Result.success(
        Working(
            startTime = startTime,
            exercisingState = exercisingState,
            durationGoal = durationGoal,
        )
    )
}

fun Working.workedFor(milliSeconds: Long): Working {
    val timeToWork = durationGoal - milliSeconds
    return copy(
        exercisingState = exercisingState.copy(
            timeLeft = if(timeToWork > 0) timeToWork else 0
        )
    )
}

fun WorkoutState.startRest(startTime: Long): Result<Resting> {
    if (this !is Working) return Result.failure(StartingRestFromWrongStateException)

    return Result.success(Resting(
        startTime = startTime,
        exercisingState = exercisingState.copy(
            timeLeft = workoutProgress.activeSet().restTime*1000L,
        ),
    ))
}

fun Resting.restedFor(milliSeconds: Long): Resting {
    val timeToRest = workoutProgress.activeSet().restTime*1000L - milliSeconds
    return copy(
        exercisingState = exercisingState.copy(
            timeLeft = if(timeToRest > 0) timeToRest else 0
        )
    )
}

fun WorkoutState.finishRest(): Result<SetFinished> {
    if (this !is IsExercisingState) return Result.failure(FinishingSetFromNoExercisingState)

    return Result.success(SetFinished(workoutProgress = workoutProgress))
}

fun SetFinished.goToNextSet(): WaitingToStartSet {
    return workoutProgress.forNextSet().toWaitingToStartSet()
}

fun SetFinished.finishExercise(): ExerciseFinished {
    return ExerciseFinished(workoutProgress = workoutProgress)
}

fun ExerciseFinished.goToNextExercise(): WaitingToStartExercise {
    return WaitingToStartExercise(workoutProgress = workoutProgress.forNextExercise())
}

fun WorkoutState.startExercise(): Result<WaitingToStartSet> {
    if (this !is WaitingToStartExercise) return Result.failure(StartingExerciseFromWrongStateException)

    return Result.success(workoutProgress.toWaitingToStartSet())
}

private fun WorkoutProgress.toWaitingToStartSet(): WaitingToStartSet {
    val currentSet = activeSet()
    val durationInMs = currentSet.duration*1000L
    return WaitingToStartSet(
        exercisingState = ExercisingState(
            workoutProgress = this,
            timeLeft = durationInMs,
            tractionGoal = currentSet.tractionGoal*1000L,
        ),
        durationGoal = durationInMs,
    )
}