package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.IsTimestampProvider
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.WorkoutStateFactory.workoutStateOfNewWorkout
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
    object FinishSet: WorkoutAction()
    object FinishExercise: WorkoutAction()
    object StartSet: WorkoutAction()
    object StartRest: WorkoutAction()
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

sealed class WorkoutStoreExceptions : Exception() {
    object WorkoutNotSetException: WorkoutStoreExceptions()
    object NoTractionGoalAtStartOfSetException: WorkoutExceptions()
    object NoDurationGoalAtStartOfSetException: WorkoutExceptions()
}

sealed class WorkoutSideEffect : Effect {
    data class Error(val error: Exception) : WorkoutSideEffect()
    object NewWorkoutStarted : WorkoutSideEffect()
    data class SetFinished(val workoutProgress: WorkoutProgress, val tractionGoal: Long) : WorkoutSideEffect()
    object WorkoutFinished : WorkoutSideEffect()
}

class WorkoutStore(
    initialState: WorkoutState = WorkoutState(),
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
                is WorkoutAction.StartWorkout -> {
                    action.workout.throwIfNotValid()

                    state.value = workoutStateOfNewWorkout(
                        workout = action.workout,
                        firstSet = action.workout.exercises.first().sets.first(),
                    )
                    launch {
                        sideEffect.emit(WorkoutSideEffect.NewWorkoutStarted)
                    }
                }
                is WorkoutAction.FinishExercise -> {
                    when {
                        state.value.workoutProgress.atLastExercise() -> launch {
                            Napier.d("Last exercise finished. Emitting WorkoutFinished effect.")
                            sideEffect.emit(WorkoutSideEffect.WorkoutFinished)
                        }
                        else -> state.value = state.value.forNextExercise()
                    }
                }
                is WorkoutAction.StartSet -> {
                    val currentTractionGoal = state.value.tractionGoal
                    val currentDurationGoal = state.value.durationGoal
                    when {
                        currentTractionGoal == null -> throw WorkoutStoreExceptions.NoTractionGoalAtStartOfSetException
                        currentDurationGoal == null -> throw WorkoutStoreExceptions.NoDurationGoalAtStartOfSetException
                        else -> {
                            // Start the set and count down time
                            startWorkCountDown(
                                currentTractionGoal = currentTractionGoal,
                                currentDurationGoal = currentDurationGoal
                            )
                        }
                    }
                }
                is WorkoutAction.StartRest -> {
                    startRestCountDown()
                }
                is WorkoutAction.FinishSet -> {
                    state.value.forNextSet()?.let {
                        state.value = it
                    } ?: dispatch(WorkoutAction.FinishExercise)
                }
                is WorkoutAction.SetDurationGoal -> {
                    if (state.value.working) return

                    state.value = state.value.copy(durationGoal = action.durationGoal)
                }
                is WorkoutAction.SetTractionGoal -> {
                    if (state.value.working) return

                    state.value = state.value.copy(tractionGoal = action.tractionGoal)
                }
            }
        } catch (exception: Exception) {
            launch {
                sideEffect.emit(WorkoutSideEffect.Error(exception))
            }
        }
    }

    private fun startWorkCountDown(
        currentTractionGoal: Long,
        currentDurationGoal: Long,
    ) {
        val (newState, workoutProgress) = state.value.forSetStart(startTime = timestampProvider.getTimeMillis())
        state.value = newState
        val workTimeCountdown = launch {
            while (state.value.timeToWork > 0) {
                delay(TICKS)
                val timeDone = timestampProvider.getTimeMillis() - state.value.startTime
                val timeToWork = currentDurationGoal - timeDone
                state.value = state.value.copy(timeToWork = if(timeToWork > 0) timeToWork else 0)
            }
        }
        workTimeCountdown.invokeOnCompletion {
            launch {
                sideEffect.emit(WorkoutSideEffect.SetFinished(
                    workoutProgress = workoutProgress,
                    tractionGoal = currentTractionGoal,
                ))
            }
            state.value = state.value.copy(working = false)
            dispatch(WorkoutAction.StartRest)
        }
    }

    private fun startRestCountDown() {
        val (newState, workoutProgress) = state.value.forRest(timestampProvider.getTimeMillis())
        state.value = newState
        val restTimeCounter = launch {
            while (state.value.timeToRest > 0) {
                delay(TICKS)
                val timeDone = timestampProvider.getTimeMillis() - state.value.startTime
                val timeToRest = workoutProgress.activeSet().restTime - timeDone
                state.value = state.value.copy(timeToRest = if(timeToRest > 0) timeToRest else 0)
            }
        }
        restTimeCounter.invokeOnCompletion {
            dispatch(WorkoutAction.FinishSet)
        }
    }

    companion object {
        const val TICKS = 10L
    }
}
