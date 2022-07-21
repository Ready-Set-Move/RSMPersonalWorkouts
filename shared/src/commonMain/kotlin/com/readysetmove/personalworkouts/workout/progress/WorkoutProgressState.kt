package com.readysetmove.personalworkouts.workout.progress

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.workout.Workout
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressAction.GoToNextExercise
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressAction.GoToNextSet
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressState.*

sealed class WorkoutProgressAction: Action {
    data class StartWorkoutProgress(val workoutStarted: WaitingToStartExercise): WorkoutProgressAction()
    data class StartExercise(val transitionToWaitingToStartSet: TransitionToWaitingToStartSet): WorkoutProgressAction()
    data class StartSet(val waitingToStartSet: WaitingToStartSet): WorkoutProgressAction()
    data class FinishWork(val working: Working): WorkoutProgressAction()
    data class FinishSet(val resting: Resting): WorkoutProgressAction()
    data class GoToNextSet(val setFinished: SetFinished): WorkoutProgressAction()
    data class GoToNextExercise(val exerciseFinished: ExerciseFinished): WorkoutProgressAction()
    data class TransitionToWaitingToStartSet(val waitingToStartSet: WaitingToStartSet): WorkoutProgressAction()
    data class SetTractionGoal(val waitingToStartSetWithUpdatedTraction: WaitingToStartSet): WorkoutProgressAction()
    data class SetDurationGoal(val waitingToStartSetWithUpdatedDuration: WaitingToStartSet): WorkoutProgressAction()
}

interface IsExercisingState {
    val workoutProgress: WorkoutProgress
    val tractionGoal: Long
    val timeLeft: Long
    val durationGoal: Long
}

data class ExercisingState(
    override val workoutProgress: WorkoutProgress,
    override val tractionGoal: Long,
    override val durationGoal: Long,
    override val timeLeft: Long,
): IsExercisingState

sealed class WorkoutProgressState : State {
    object NoWorkout : WorkoutProgressState()
    data class WaitingToStartExercise(
        val workoutProgress: WorkoutProgress,
    ) : WorkoutProgressState()
    data class WaitingToStartSet(
        val exercisingState: ExercisingState,
    ): WorkoutProgressState(), IsExercisingState by exercisingState
    data class Working(
        val startTime: Long,
        val exercisingState: ExercisingState,
    ) : WorkoutProgressState(), IsExercisingState by exercisingState
    data class Resting(
        val startTime: Long,
        val exercisingState: ExercisingState,
    ) : WorkoutProgressState(), IsExercisingState by exercisingState
    data class SetFinished(
        val exercisingState: ExercisingState,
    ) : WorkoutProgressState(), IsExercisingState by exercisingState
    data class ExerciseFinished(
        val workoutProgress: WorkoutProgress,
    ) : WorkoutProgressState()
    data class WorkoutFinished(val workout: Workout) : WorkoutProgressState()
}

// TODO: correct usage later on when starting workout from screen
fun NoWorkout.startWorkoutAction(workout: Workout): WorkoutProgressAction.StartWorkoutProgress {
    return WorkoutProgressAction.StartWorkoutProgress(
        workoutStarted = WaitingToStartExercise(
            workoutProgress = WorkoutProgress(
                workout = workout,
            ),
        ))
}

fun WaitingToStartExercise.startExerciseAction(): WorkoutProgressAction.StartExercise {
    return WorkoutProgressAction.StartExercise(
        transitionToWaitingToStartSet = WorkoutProgressAction.TransitionToWaitingToStartSet(
            waitingToStartSet = workoutProgress.toWaitingToStartSet()
        )
    )
}

fun WaitingToStartSet.setDurationGoalAction(durationGoal: Long): WorkoutProgressAction.SetDurationGoal {
    return WorkoutProgressAction.SetDurationGoal(
        waitingToStartSetWithUpdatedDuration = copy(
            exercisingState = exercisingState.copy(durationGoal = durationGoal)
        )
    )
}

fun WaitingToStartSet.setTractionGoalAction(tractionGoal: Long): WorkoutProgressAction.SetTractionGoal {
    return WorkoutProgressAction.SetTractionGoal(
        waitingToStartSetWithUpdatedTraction = copy(
            exercisingState = exercisingState.copy(tractionGoal = tractionGoal)
        )
    )
}

fun WaitingToStartSet.startSetAction(): WorkoutProgressAction.StartSet {
    return WorkoutProgressAction.StartSet(waitingToStartSet = this)
}
fun WaitingToStartSet.startSet(startTime: Long): Working {
    return Working(
        startTime = startTime,
        exercisingState = exercisingState,
    )
}
// internal usage, no action
fun Working.workedFor(milliSeconds: Long): Working {
    val timeToWork = durationGoal - milliSeconds
    return copy(
        exercisingState = exercisingState.copy(
            timeLeft = if(timeToWork > 0) timeToWork else 0
        )
    )
}

fun Working.finishWorkAction(): WorkoutProgressAction.FinishWork {
    return WorkoutProgressAction.FinishWork(working = this)
}
fun Working.startRest(startTime: Long): Resting {
    return Resting(
        startTime = startTime,
        exercisingState = exercisingState.copy(
            timeLeft = workoutProgress.activeSet().restTime*1000L,
        ),
    )
}
// internal usage, no action
fun Resting.restedFor(milliSeconds: Long): Resting {
    val timeToRest = workoutProgress.activeSet().restTime*1000L - milliSeconds
    return copy(
        exercisingState = exercisingState.copy(
            timeLeft = if(timeToRest > 0) timeToRest else 0
        )
    )
}

fun Resting.finishSetAction(): WorkoutProgressAction.FinishSet {
    return WorkoutProgressAction.FinishSet(resting = this)
}
fun Resting.finishRest(): SetFinished {
    return SetFinished(exercisingState = exercisingState.copy(
        timeLeft = 0
    ))
}

fun SetFinished.goToNextSetAction(): GoToNextSet {
    return GoToNextSet(
        setFinished = this,
    )
}
fun SetFinished.goToNextSet(): WaitingToStartSet {
    return workoutProgress.forNextSet().toWaitingToStartSet()
}
fun SetFinished.finishExercise(): ExerciseFinished {
    return ExerciseFinished(workoutProgress = workoutProgress)
}

fun ExerciseFinished.goToNextExerciseAction(): GoToNextExercise {
    return GoToNextExercise(
        exerciseFinished = this,
    )
}
fun ExerciseFinished.goToNextExercise(): WaitingToStartExercise {
    return WaitingToStartExercise(workoutProgress = workoutProgress.forNextExercise())
}
fun ExerciseFinished.finishWorkout(): WorkoutFinished {
    return WorkoutFinished(workout = workoutProgress.workout)
}

private fun WorkoutProgress.toWaitingToStartSet(): WaitingToStartSet {
    val currentSet = activeSet()
    val durationInMs = currentSet.duration*1000L
    return WaitingToStartSet(
        exercisingState = ExercisingState(
            workoutProgress = this,
            timeLeft = durationInMs,
            tractionGoal = currentSet.tractionGoal*1000L,
            durationGoal = durationInMs,
        ),
    )
}