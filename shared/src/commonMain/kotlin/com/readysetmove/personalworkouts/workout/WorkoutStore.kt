package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.IsTimestampProvider
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

sealed class WorkoutExceptions : Exception() {
    data class EmptyWorkoutException(val workout: Workout): WorkoutExceptions()
    data class EmptyExerciseException(
        val workout: Workout,
        val exercise: Exercise,
    ): WorkoutExceptions()
}

// TODO: which of these do we actually need?
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
    mainDispatcher: CoroutineContext,
//    private val workoutResultsStore: WorkoutResultsStore,
    private val timestampProvider: IsTimestampProvider,
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
                    state.value = action.workoutStarted
                    launch {
                        sideEffect.emit(WorkoutSideEffect.NewWorkoutStarted)
                    }
                }
                is StartExercise -> {
                    state.value = action.exerciseStarted.apply {
                        launch {
                            sideEffect.emit(WorkoutSideEffect.NewSetActivated(workoutProgress))
                        }
                    }
                }
                is StartSet -> {
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
                    state.value = action.resting.finishRest()
                }
                // TODO: Skip set
                is RateSet -> {
                    state.value = action.setFinished.let { setFinishedState ->
                        launch {
                            // TODO: directly store result
                            sideEffect.emit(WorkoutSideEffect.SetRated(
                                workoutProgress = setFinishedState.workoutProgress,
                                rating = action.rating)
                            )
                        }

                        when {
                            setFinishedState.workoutProgress.atLastSetOfExercise() -> {
                                setFinishedState.finishExercise()
                            }
                            else -> {
                                setFinishedState.goToNextSet().apply {
                                    launch {
                                        sideEffect.emit(WorkoutSideEffect.NewSetActivated(workoutProgress))
                                    }
                                }
                            }
                        }
                    }
                }
                is RateExercise -> {
                    state.value = action.exerciseFinished.let { exerciseFinishedState ->
                        launch {
                            // TODO: directly store result
                            sideEffect.emit(WorkoutSideEffect.ExerciseRated(
                                workoutProgress = exerciseFinishedState.workoutProgress,
                                rating = action.rating)
                            )
                        }

                        when {
                            exerciseFinishedState.workoutProgress.atLastSetOfWorkout() -> {
                                launch {
                                    Napier.d("Last exercise finished. Emitting WorkoutFinished effect.")
                                    sideEffect.emit(WorkoutSideEffect.WorkoutFinished)
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
                sideEffect.emit(WorkoutSideEffect.Error(exception))
            }
        }
    }

    private fun startWorkCountDown(
        workingState: WorkoutState.Working
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
                sideEffect.emit(WorkoutSideEffect.WorkFinished(
                    workoutProgress = workingState.workoutProgress,
                    tractionGoal = workingState.tractionGoal,
                ))
            }
            dispatch(workingState.finishWorkAction())
        }
    }

    private fun startRestCountDown(restingState: WorkoutState.Resting) {
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
