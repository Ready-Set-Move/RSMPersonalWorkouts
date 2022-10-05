package com.readysetmove.personalworkouts.workout.progress

import com.readysetmove.personalworkouts.IsTimestampProvider
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.Exercise
import com.readysetmove.personalworkouts.workout.Workout
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressAction.*
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressSideEffect.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

sealed class WorkoutExceptions : Exception() {
    data class EmptyWorkoutException(val workout: Workout): WorkoutExceptions()
    data class EmptyExerciseException(
        val workout: Workout,
        val exercise: Exercise,
    ): WorkoutExceptions()
}

// TODO: which of these do we actually need?
sealed class WorkoutProgressSideEffect : Effect {
    data class Error(val error: Exception) : WorkoutProgressSideEffect()
    object NewWorkoutProgressStarted : WorkoutProgressSideEffect()
    data class WorkFinished(val workoutProgress: WorkoutProgress, val tractionGoal: Long) : WorkoutProgressSideEffect()
    data class NewSetActivated(val tractionGoal: Long) : WorkoutProgressSideEffect()
    object WorkoutFinished : WorkoutProgressSideEffect()
}

// TODO: revert action flow: Don't listen in app store to side effects instead explicitly
//  trigger app store actions from here
class WorkoutProgressStore(
    initialState: WorkoutProgressState = WorkoutProgressState.NoWorkout,
    mainDispatcher: CoroutineContext,
    private val timestampProvider: IsTimestampProvider,
):
    Store<WorkoutProgressState, WorkoutProgressAction, WorkoutProgressSideEffect>,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(initialState)
    private val sideEffect = MutableSharedFlow<WorkoutProgressSideEffect>()
    override fun observeState(): StateFlow<WorkoutProgressState> = state
    override fun observeSideEffect(): Flow<WorkoutProgressSideEffect> = sideEffect

    override fun dispatch(action: WorkoutProgressAction) {
        try {
            when(action) {
                is StartWorkoutProgress -> {
                    // TODO: workout time metrics (start + finish)
                    state.value = action.workoutStarted
                    launch {
                        sideEffect.emit(NewWorkoutProgressStarted)
                    }
                }
                is StartExercise -> {
                    dispatch(action.transitionToWaitingToStartSet)
                }
                is TransitionToWaitingToStartSet -> {
                    state.value = action.waitingToStartSet.apply {
                        launch {
                            sideEffect.emit(NewSetActivated(tractionGoal = tractionGoal))
                        }
                    }
                }
                is StartSet -> {
                    // TODO: skip set for non tracking exercises (body weight, without device)
                    state.value = action.waitingToStartSet
                        .startSet(startTime = timestampProvider.getTimeMillis())
                        .apply {
                            startWorkCountDown(this)
                        }
                }
                is FinishWork -> {
                    state.value = action.working
                        .startRest(startTime = timestampProvider.getTimeMillis())
                        .apply {
                            startRestCountDown(this)
                        }
                }
                is FinishSet -> {
                    // TODO: check for warm up sets and skip set finished state (results + rating screen)
                    state.value = action.resting.finishRest()
                }
                is GoToNextSet -> {
                    action.setFinished.let { setFinishedState ->
                        when {
                            setFinishedState.workoutProgress.atLastSetOfExercise() -> {
                                state.value = setFinishedState.finishExercise()
                            }
                            else -> {
                                dispatch(TransitionToWaitingToStartSet(
                                    waitingToStartSet = setFinishedState.goToNextSet()
                                ))
                            }
                        }
                    }
                }
                is GoToNextExercise -> {
                    state.value = action.exerciseFinished.let { exerciseFinishedState ->
                        when {
                            exerciseFinishedState.workoutProgress.atLastSetOfWorkout() -> {
                                Napier.d("Last exercise finished.")
                                launch {
                                    sideEffect.emit(WorkoutFinished)
                                }
                                exerciseFinishedState.finishWorkout ()
                            }
                            else -> exerciseFinishedState.goToNextExercise()
                        }
                    }
                }
                is SetDurationGoal -> {
                    state.value = action.waitingToStartSetWithUpdatedDuration
                }
                is SetTractionGoal -> {
                    state.value = action.waitingToStartSetWithUpdatedTraction
                }
            }
        } catch (exception: Exception) {
            launch {
                sideEffect.emit(Error(exception))
            }
        }
    }

    private fun startWorkCountDown(
        workingState: WorkoutProgressState.Working
    ) {
        val workTimeCountdown = launch {
            var timeToWork = workingState.timeLeft
            while (timeToWork > 0) {
                delay(TICKS)
                val timePassedSinceSetStart = timestampProvider.getTimeMillis() - workingState.startTime
                state.value = workingState
                    .workedFor(milliSeconds = timePassedSinceSetStart)
                    .apply {
                        timeToWork = timeLeft
                    }
            }
        }
        workTimeCountdown.invokeOnCompletion {
            launch {
                sideEffect.emit(WorkFinished(
                    workoutProgress = workingState.workoutProgress,
                    tractionGoal = workingState.tractionGoal,
                ))
            }
            dispatch(workingState.finishWorkAction())
        }
    }

    private fun startRestCountDown(restingState: WorkoutProgressState.Resting) {
        val restTimeCounter = launch {
            var timeToRest = restingState.timeLeft
            while (timeToRest > 0) {
                delay(TICKS)
                val timePassedSinceRestStart = timestampProvider.getTimeMillis() - restingState.startTime
                state.value = restingState
                    .restedFor(milliSeconds = timePassedSinceRestStart)
                    .apply {
                        timeToRest = timeLeft
                    }
            }
        }
        restTimeCounter.invokeOnCompletion {
            dispatch(restingState.finishSetAction())
        }
    }

    companion object {
        const val TICKS = 10L
    }
}
