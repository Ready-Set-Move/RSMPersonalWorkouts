package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.device.DeviceAction
import com.readysetmove.personalworkouts.device.DeviceStore
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class WorkoutProgress(
    val workout: Workout,
    val activeExercise: Exercise,
    val activeSet: Set,
)

data class WorkoutState(
    val workoutProgress: WorkoutProgress? = null,
    val timeToWork: Long = 0,
    val timeToRest: Long = 0,
    val tractionGoal: Long? = null,
    val working: Boolean = false,
) : State

sealed class WorkoutAction: Action {
    data class StartWorkout(val workout: Workout): WorkoutAction()
    object FinishExercise: WorkoutAction()
    object StartSet: WorkoutAction()
    object StartRest: WorkoutAction()
    data class SetTractionGoal(val tractionGoal: Long): WorkoutAction()
    data class SetDurationGoal(val durationGoal: Long): WorkoutAction()
}

sealed class WorkoutExceptions : Exception() {
    data class EmptyWorkoutException(val workout: Workout): WorkoutExceptions()
    data class EmptyExcerciseException(
        val workout: Workout,
        val exercise: Exercise,
    ): WorkoutExceptions()
}

sealed class WorkoutStoreExceptions : Exception() {
    object WorkoutNotSetException: WorkoutStoreExceptions()
}

class WorkoutStore(
    initialState: WorkoutState = WorkoutState(),
    private val deviceStore: DeviceStore,
    private val mainDispatcher: CoroutineContext,
):
    Store<WorkoutState, WorkoutAction, WorkoutSideEffect>,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(initialState)
    private val sideEffect = MutableSharedFlow<WorkoutSideEffect>()
    override fun observeState(): StateFlow<WorkoutState> = state
    override fun observeSideEffect(): Flow<WorkoutSideEffect> = sideEffect

    private fun launchError(error: WorkoutSideEffect.Error) {
        launch {
            sideEffect.emit(error)
        }
    }

    private val TICKS = 10L

    override fun dispatch(action: WorkoutAction) {
        val check4Workout = {
            if (state.value.workoutProgress == null) {
                launchError(WorkoutSideEffect.Error(WorkoutStoreExceptions.WorkoutNotSetException))
            }
            state.value.workoutProgress
        }
        when(action) {
            is WorkoutAction.StartWorkout -> {
                if (!action.workout.isValid(launchError = this::launchError)) return

                val firstExercise = action.workout.exercises.first()
                val firstSet = firstExercise.sets.first()
                state.value = WorkoutState(
                    workoutProgress = WorkoutProgress(
                        workout = action.workout,
                        activeExercise = firstExercise,
                        activeSet = firstSet,
                    ),
                    timeToWork = firstSet.duration,
                    timeToRest = 0,
                    tractionGoal = firstSet.tractionGoal,
                )
            }
            is WorkoutAction.FinishExercise -> {
                check4Workout()?.let { workoutProgress ->
                    // already last exercise?
                    if (workoutProgress.activeExercise == workoutProgress.workout.exercises.last()) {
                        launch {
                            sideEffect.emit(WorkoutSideEffect.WorkoutFinished("RESULTS"))
                        }
                        return
                    }

                    val nextExercise = workoutProgress.workout.exercises[
                            workoutProgress.workout.exercises.indexOf(workoutProgress.activeExercise) + 1
                        ]
                    val activeSet = nextExercise.sets[0].copy()
                    state.value = state.value.copy(
                        workoutProgress = workoutProgress.copy(
                            activeExercise = nextExercise,
                            activeSet = activeSet,
                        ),
                        timeToRest = 0,
                        timeToWork = activeSet.duration,
                        tractionGoal = activeSet.tractionGoal,
                    )
                }
            }
            is WorkoutAction.StartSet -> {
                check4Workout()?.let { workoutProgress ->
                    state.value = state.value.copy(working = true)
                    // Start the set and count down time TODO: start tracking when in traction range
                    deviceStore.dispatch(DeviceAction.StartTracking)
                    // count down work time
                    val workTimeCountdown = launch {
                        while (state.value.timeToWork > 0) {
                            delay(TICKS)
                            state.value = state.value.copy(timeToWork = state.value.timeToWork - TICKS)
                        }
                    }
                    workTimeCountdown.invokeOnCompletion {
                        deviceStore.dispatch(DeviceAction.StopTracking)
                        // TODO: store results
                        dispatch(WorkoutAction.StartRest)
                        state.value = state.value.copy(working = false)
                    }
                }
            }
            is WorkoutAction.StartRest -> {
                check4Workout()?.let { workoutProgress ->
                    state.value = state.value.copy(
                        timeToRest = workoutProgress.activeSet.restTime,
                        timeToWork = 0,
                    )
                    val restTimeCounter = launch {
                        while (state.value.timeToRest > 0) {
                            delay(TICKS)
                            state.value = state.value.copy(timeToRest = state.value.timeToRest - TICKS)
                        }
                    }
                    restTimeCounter.invokeOnCompletion {
                        dispatch(WorkoutAction.FinishExercise)
                    }
                }
            }
            is WorkoutAction.SetDurationGoal -> {
                if (state.value.working) return
                check4Workout()?.let {
                    state.value = state.value.copy(timeToWork = action.durationGoal)
                }
            }
            is WorkoutAction.SetTractionGoal -> {
                if (state.value.working) return
                    state.value = state.value.copy(tractionGoal = action.tractionGoal)
            }
        }
    }
}

sealed class WorkoutSideEffect : Effect {
    data class Error(val error: Exception) : WorkoutSideEffect()
    data class WorkoutFinished(val results: String) : WorkoutSideEffect()
}

fun Workout.isValid(launchError: (WorkoutSideEffect.Error) -> Unit): Boolean {
    if (exercises.isEmpty()) {
        launchError(WorkoutSideEffect.Error(
            WorkoutExceptions.EmptyWorkoutException(this)
        ))
        return false
    }
    exercises.forEach { exercise ->
        if (exercise.sets.isEmpty()) {
            launchError(WorkoutSideEffect.Error(
                WorkoutExceptions.EmptyExcerciseException(
                    workout = this,
                    exercise = exercise,
                )
            ))
            return false
        }
    }
    return true
}

