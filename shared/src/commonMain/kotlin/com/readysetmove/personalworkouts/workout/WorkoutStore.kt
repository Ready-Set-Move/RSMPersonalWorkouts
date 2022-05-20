package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.device.DeviceAction
import com.readysetmove.personalworkouts.device.DeviceStore
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class WorkoutState(
    val workout: Workout? = null,
    val activeExercise: Exercise? = null,
    val activeSet: Set? = null,
    val timeToWork: Float = 0f,
    val timeToRest: Float = 0f,
    val working: Boolean = false,
) : State

sealed class WorkoutAction: Action {
    data class SetWorkout(val workout: Workout): WorkoutAction()
    object NextExercise: WorkoutAction()
    object StartSet: WorkoutAction()
    object StartRest: WorkoutAction()
    data class SetTractionGoal(val tractionGoal: Float): WorkoutAction()
    data class SetDurationGoal(val durationGoal: Float): WorkoutAction()
}

sealed class WorkoutSideEffect : Effect {
    data class Error(val error: Exception) : WorkoutSideEffect()
    data class WorkoutFinished(val results: String) : WorkoutSideEffect()
}

class WorkoutStore(
    private val deviceStore: DeviceStore,
    private val mainDispatcher: CoroutineContext,
    private val ioDispatcher: CoroutineContext,
):
    Store<WorkoutState, WorkoutAction, WorkoutSideEffect>,
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val state = MutableStateFlow(WorkoutState())
    private val sideEffect = MutableSharedFlow<WorkoutSideEffect>()
    override fun observeState(): StateFlow<WorkoutState> = state
    override fun observeSideEffect(): Flow<WorkoutSideEffect> = sideEffect

    private var currentExerciseIndex = 0
    private var currentSetIndex = 0

    override fun dispatch(action: WorkoutAction) {
        val check4Set = {
            if (state.value.activeSet == null) {
                launch(mainDispatcher) {
                    sideEffect.emit(WorkoutSideEffect.Error(Exception("No set active.")))
                }
            }
            state.value.activeSet
        }
        val check4Workout = {
            if (state.value.workout == null) {
                launch(mainDispatcher) {
                    sideEffect.emit(WorkoutSideEffect.Error(Exception("Workout not set.")))
                }
            }
            state.value.workout
        }
        when(action) {
            is WorkoutAction.SetWorkout -> {
                currentExerciseIndex = 0
                currentSetIndex = 0
                val firstExercise =
                    if (action.workout.exercises.isEmpty()) null
                    else action.workout.exercises[0]
                val firstSet = firstExercise?.let {
                    if (it.sets.isEmpty()) null
                    else it.sets[0]
                }
                state.value = WorkoutState(
                    workout = action.workout,
                    activeExercise = firstExercise,
                    activeSet = firstSet
                )
            }
            is WorkoutAction.NextExercise -> {
                check4Workout()?.let { workout ->
                    val newExerciseIndex = currentExerciseIndex + 1
                    if (workout.exercises.size > newExerciseIndex) {
                        currentExerciseIndex = newExerciseIndex
                        val activeExercise = workout.exercises[newExerciseIndex].copy()
                        if (activeExercise.sets.isEmpty()) {
                            launch(mainDispatcher) {
                                sideEffect.emit(WorkoutSideEffect.Error(Exception("Exercise without sets.")))
                            }
                            return
                        }
                        val activeSet = activeExercise.sets[0].copy()
                        state.value = state.value.copy(
                            activeExercise = activeExercise,
                            activeSet = activeSet,
                            timeToRest = 0f,
                            timeToWork = activeSet.duration,
                        )
                    }
                }
            }
            is WorkoutAction.StartSet -> {
                check4Workout()?.let { workout ->
                    // Start the set and count down time
                    deviceStore.dispatch(DeviceAction.StartTracking)
                    // count down work time
                    val workTimeCountdown = launch(ioDispatcher) {
                        while (state.value.timeToWork > 0f) {
                            delay(10)
                            state.value = state.value.copy(timeToWork = state.value.timeToWork - .01f)
                        }
                    }
                    workTimeCountdown.invokeOnCompletion {
                        deviceStore.dispatch(DeviceAction.StopTracking)
                        // TODO: store results
                        // last set of last exercise?
                        if (
                            currentExerciseIndex == workout.exercises.size - 1 &&
                            currentSetIndex == workout.exercises[currentExerciseIndex].sets.size - 1
                        ) {
                            launch(mainDispatcher) {
                                sideEffect.emit(WorkoutSideEffect.WorkoutFinished("RESULTS"))
                            }
                        } else {
                            dispatch(WorkoutAction.StartRest)
                        }
                        state.value = state.value.copy(working = false)
                    }
                    state.value = state.value.copy(working = true)
                }
            }
            is WorkoutAction.StartRest -> {
                check4Set()?.let { set ->
                    state.value = state.value.copy(timeToRest = set.restTime)
                    val restTimeCounter = launch(ioDispatcher) {
                        while (state.value.timeToRest > 0f) {
                            delay(10)
                            state.value = state.value.copy(timeToRest = state.value.timeToRest - .01f)
                        }
                    }
                    restTimeCounter.invokeOnCompletion {
                        dispatch(WorkoutAction.NextExercise)
                    }
                }
            }
            is WorkoutAction.SetDurationGoal -> {
                if (state.value.working) return
                check4Set()?.let {
                    state.value = state.value.copy(
                        timeToWork = action.durationGoal
                    )
                }
            }
            is WorkoutAction.SetTractionGoal -> {
                if (state.value.working) return
                check4Set()?.let { set ->
                    state.value = state.value.copy(
                        activeSet = set.copy(tractionGoal = action.tractionGoal)
                    )
                }

            }
        }
    }
}