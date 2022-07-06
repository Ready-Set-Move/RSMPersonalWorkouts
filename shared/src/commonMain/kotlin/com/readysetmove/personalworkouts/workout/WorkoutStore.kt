package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.IsTimestampProvider
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.WorkoutAction.*
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
    object FinishWorkout: WorkoutAction()
    data class GoToSet(val workoutProgress: WorkoutProgress): WorkoutAction()
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
    data class WorkFinished(val workoutProgress: WorkoutProgress, val tractionGoal: Long) : WorkoutSideEffect()
    data class NewSetActivated(val workoutProgress: WorkoutProgress) : WorkoutSideEffect()
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
                is StartWorkout -> {
                    action.workout.throwIfNotValid()

                    state.value = workoutStateOfNewWorkout(
                        workout = action.workout,
                        firstSet = action.workout.exercises.first().sets.first(),
                    )
                    launch {
                        sideEffect.emit(WorkoutSideEffect.NewWorkoutStarted)
                    }
                }
                is GoToSet -> {
                    state.value = state.value.forWorkoutProgress(action.workoutProgress)
                    launch {
                        sideEffect.emit(WorkoutSideEffect.NewSetActivated(action.workoutProgress))
                    }
                }
                is FinishWorkout -> {
                    launch {
                        Napier.d("Last exercise finished. Emitting WorkoutFinished effect.")
                        sideEffect.emit(WorkoutSideEffect.WorkoutFinished)
                    }
                }
                is StartSet -> {
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
                is StartRest -> {
                    startRestCountDown()
                }
                is FinishSet -> {
                    state.value.workoutProgress?.let { workoutProgress ->
                        // get progress for next set or exercise
                        dispatch(GoToSet(workoutProgress.forNextStep()))

                        if (workoutProgress.atLastSetOfWorkout()) {
                            dispatch(FinishWorkout)
                        }
                    } ?: throw WorkoutStoreExceptions.WorkoutNotSetException
                }
                is SetDurationGoal -> {
                    if (state.value.working) return

                    state.value = state.value.copy(durationGoal = action.durationGoal)
                }
                is SetTractionGoal -> {
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
                sideEffect.emit(WorkoutSideEffect.WorkFinished(
                    workoutProgress = workoutProgress,
                    tractionGoal = currentTractionGoal,
                ))
            }
            state.value = state.value.copy(working = false)
            dispatch(StartRest)
        }
    }

    private fun startRestCountDown() {
        val (newState, workoutProgress) = state.value.forRest(timestampProvider.getTimeMillis())
        state.value = newState
        val restTimeCounter = launch {
            val restDuration = workoutProgress.activeSet().restTime*1000L
            while (state.value.timeToRest > 0) {
                delay(TICKS)
                val timeDone = timestampProvider.getTimeMillis() - state.value.startTime
                val timeToRest = restDuration - timeDone
                state.value = state.value.copy(timeToRest = if(timeToRest > 0) timeToRest else 0)
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
