package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.IsTimestampProvider
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.WorkoutAction.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

sealed class WorkoutAction: Action {
    data class StartWorkout(val workout: Workout): WorkoutAction()
    object StartExercise: WorkoutAction()
    object StartSet: WorkoutAction()
    object FinishWork: WorkoutAction()
    object FinishSet: WorkoutAction()
    data class RateSet(val rating: Int): WorkoutAction()
    data class RateExercise(val rating: Int): WorkoutAction()
    object FinishWorkout: WorkoutAction()
    data class SetTractionGoal(val tractionGoal: Long): WorkoutAction()
    data class SetDurationGoal(val durationGoal: Long): WorkoutAction()
}

sealed class WorkoutExceptions : Exception() {
    data class EmptyWorkoutException(val workout: Workout): WorkoutExceptions()
    data class EmptyExerciseException(
        val workout: Workout,
        val exercise: Exercise,
    ): WorkoutExceptions()
}

sealed class WorkoutSideEffect : Effect {
    data class Error(val error: Exception) : WorkoutSideEffect()
    object NewWorkoutStarted : WorkoutSideEffect()
    data class WorkFinished(val workoutProgress: WorkoutProgress, val tractionGoal: Long) : WorkoutSideEffect()
    data class SetRated(val workoutProgress: WorkoutProgress, val rating: Int) : WorkoutSideEffect()
    data class ExerciseRated(val workoutProgress: WorkoutProgress, val rating: Int) : WorkoutSideEffect()
    data class NewSetActivated(val workoutProgress: WorkoutProgress) : WorkoutSideEffect()
    object WorkoutFinished : WorkoutSideEffect()
}

// TODO: revert action flow: Don't listen in app store to side effects instead explicitly
//  trigger app store actions from here
class WorkoutStore(
    initialState: WorkoutState = WorkoutState.NoWorkout,
    private val timestampProvider: IsTimestampProvider,
    private val mainDispatcher: CoroutineContext,
):
    Store<WorkoutState, WorkoutAction, WorkoutSideEffect>,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(initialState)
    private val sideEffect = MutableSharedFlow<WorkoutSideEffect>()
    override fun observeState(): StateFlow<WorkoutState> = state
    override fun observeSideEffect(): Flow<WorkoutSideEffect> = sideEffect

    override fun dispatch(action: WorkoutAction) {
        try {
            when(action) {
                is StartWorkout -> {
                    // TODO: workout time metrics (start + finish)
                    // TODO: switch to result pattern
                    action.workout.throwIfNotValid()

                    state.value = state.value.startWorkout(workout = action.workout).getOrThrow()
                    launch {
                        sideEffect.emit(WorkoutSideEffect.NewWorkoutStarted)
                    }
                }
                is StartExercise -> {
                    state.value = state.value.startExercise().getOrThrow().apply {
                        launch {
                            sideEffect.emit(WorkoutSideEffect.NewSetActivated(workoutProgress))
                        }
                    }
                }
                is StartSet -> {
                    startWorkCountDown(
                        state.value.startSet(startTime = timestampProvider.getTimeMillis())
                            .getOrThrow()
                    )
                }
                is FinishWork -> {
                    startRestCountDown(
                        state.value.startRest(startTime = timestampProvider.getTimeMillis())
                            .getOrThrow()
                    )
                }
                is FinishSet -> {
                    state.value = state.value.finishRest().getOrThrow()
                }
                // TODO: Skip set
                is RateSet -> {
                    state.value.let {
                        if (it !is WorkoutState.SetFinished) {
                            // TODO: error
                            return
                        }

                        launch {
                            // TODO: directly store result
                            sideEffect.emit(WorkoutSideEffect.SetRated(
                                workoutProgress = it.workoutProgress,
                                rating = action.rating)
                            )
                        }

                        state.value = when {
                            it.workoutProgress.atLastSetOfExercise() -> {
                                it.finishExercise()
                            }
                            else -> {
                                it.goToNextSet().apply {
                                    launch {
                                        sideEffect.emit(WorkoutSideEffect.NewSetActivated(workoutProgress))
                                    }
                                }
                            }
                        }
                    }
                }
                is RateExercise -> {
                    state.value.let {
                        if (it !is WorkoutState.ExerciseFinished) {
                            // TODO: error
                            return
                        }

                        launch {
                            // TODO: directly store result
                            sideEffect.emit(WorkoutSideEffect.ExerciseRated(
                                workoutProgress = it.workoutProgress,
                                rating = action.rating)
                            )
                        }

                        if (it.workoutProgress.atLastSetOfWorkout()) {
                            dispatch(FinishWorkout)
                            return
                        }
                        state.value = it.goToNextExercise()
                    }
                }
                is FinishWorkout -> {
                    state.value = WorkoutState.WorkoutFinished
                    launch {
                        Napier.d("Last exercise finished. Emitting WorkoutFinished effect.")
                        sideEffect.emit(WorkoutSideEffect.WorkoutFinished)
                    }
                }
                is SetDurationGoal -> {
                    state.value.let {
                        if (it is WorkoutState.WaitingToStartSet)
                            state.value = it.setDurationGoal( durationGoal = action.durationGoal)
                    }
                }
                is SetTractionGoal -> {
                    state.value.let {
                        if (it is WorkoutState.WaitingToStartSet)
                            state.value = it.setTractionGoal( tractionGoal = action.tractionGoal)
                    }
                }
            }
        } catch (exception: Exception) {
            launch {
                sideEffect.emit(WorkoutSideEffect.Error(exception))
            }
        }
    }

    private fun startWorkCountDown(
        workingState: WorkoutState.Working
    ) {
        state.value = workingState
        val workTimeCountdown = launch {
            var timeLeft = workingState.timeLeft
            while (timeLeft > 0) {
                delay(TICKS)
                val timePassedSinceSetStart = timestampProvider.getTimeMillis() - workingState.startTime
                val progressedWorkingState = workingState.workedFor(milliSeconds = timePassedSinceSetStart)
                state.value = progressedWorkingState
                timeLeft = progressedWorkingState.timeLeft
            }
        }
        workTimeCountdown.invokeOnCompletion {
            launch {
                sideEffect.emit(WorkoutSideEffect.WorkFinished(
                    workoutProgress = workingState.workoutProgress,
                    tractionGoal = workingState.tractionGoal,
                ))
            }
            dispatch(FinishWork)
        }
    }

    private fun startRestCountDown(restingState: WorkoutState.Resting) {
        state.value = restingState
        val restTimeCounter = launch {
            var timeToRest = restingState.timeLeft
            while (timeToRest > 0) {
                delay(TICKS)
                val timePassedSinceRestStart = timestampProvider.getTimeMillis() - restingState.startTime
                val progressedState = restingState.restedFor(milliSeconds = timePassedSinceRestStart)
                state.value = progressedState
                timeToRest = progressedState.timeLeft
            }
        }
        restTimeCounter.invokeOnCompletion {
            dispatch(FinishSet)
        }
    }

    companion object {
        const val TICKS = 10L
    }
}
