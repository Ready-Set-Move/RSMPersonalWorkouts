package com.readysetmove.personalworkouts.app

import com.readysetmove.personalworkouts.device.DeviceAction
import com.readysetmove.personalworkouts.device.DeviceSideEffect
import com.readysetmove.personalworkouts.device.DeviceState
import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class SetResult(val tractionGoal: Long, val tractions: List<Traction>)

data class WorkoutResults(val workoutId: String, val exercises: Map<String, Map<Int, SetResult>>)

data class AppState(
    val workout: Workout? = null,
    val workoutResults: WorkoutResults? = null,
    val isWaitingToHitTractionGoal: Boolean = false,
) : State

sealed class AppAction: Action {
    object StartWorkout: AppAction()
    object StartNextSet: AppAction()
}

sealed class AppSideEffect : Effect {
    object NoWorkoutSet : AppSideEffect()
}

class AppStore(
    initialState: AppState = AppState(),
    private val deviceStore: Store<DeviceState, DeviceAction, DeviceSideEffect>,
    private val workoutStore: WorkoutStore,
    private val mainDispatcher: CoroutineContext,
):
    Store<AppState, AppAction, AppSideEffect>,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(initialState)
    private val sideEffect = MutableSharedFlow<AppSideEffect>()
    override fun observeState(): StateFlow<AppState> = state
    override fun observeSideEffect(): Flow<AppSideEffect> = sideEffect

    init {
        launch {
            // TODO: here we need to fetch the workout via the repository
            state.value = AppState(workout = EntityMocks.WORKOUT)
        }
    }

    override fun dispatch(action: AppAction) {
        when(action) {
            is AppAction.StartWorkout -> {
                val workout = state.value.workout
                if (workout == null) {
                    launch {
                        sideEffect.emit(AppSideEffect.NoWorkoutSet)
                    }
                    return
                }
                workoutStore.dispatch(WorkoutAction.StartWorkout(workout))
            }
            is AppAction.StartNextSet -> {
                launch {
                    // start tracking as soon as we have relevant traction delivered
                    deviceStore.observeState().first { deviceState ->
                        // TODO: what if we don't hit the minimal goal?
                        deviceState.traction >= 5
                    }
                    workoutStore.dispatch(WorkoutAction.StartSet)
                }
                startTracking()
                state.value = state.value.copy(isWaitingToHitTractionGoal = true)
            }
        }
    }

    private fun startTracking() {
        launch {
            workoutStore.observeSideEffect()
                .filterIsInstance<WorkoutSideEffect.SetFinished>()
                .first { setFinishedEffect ->
                    deviceStore.dispatch(DeviceAction.StopTracking)
                    val stoppedState = deviceStore.observeState().first { deviceState -> !deviceState.trackingActive }
                    val workoutResults = updateStateWithResults(
                        tractions = stoppedState.trackedTraction,
                        workoutProgress = setFinishedEffect.workoutProgress,
                        tractionGoal = setFinishedEffect.tractionGoal,
                    )
                    storeResults(workoutResults = workoutResults)
                    true
                }
        }
        deviceStore.dispatch(DeviceAction.StartTracking)
    }

    private fun updateStateWithResults(tractions: List<Traction>, tractionGoal: Long, workoutProgress: WorkoutProgress): WorkoutResults {
        val exercise = workoutProgress.activeExercise()
        val currentWorkoutResults = state.value.workoutResults
        // tractions need timestamps relative to beginning of first traction
        val setStart = tractions.first().timestamp
        val timedTractions = tractions.map {
            it.copy(timestamp = it.timestamp - setStart)
        }
        val exerciseResults = mapOf(workoutProgress.activeSetIndex to SetResult(
            tractionGoal = tractionGoal,
            tractions = timedTractions
        ))
        // no results yet? create everything from scratch
        if (currentWorkoutResults == null) {
            val exercises = mapOf(exercise.name to exerciseResults)
            val workoutResults = WorkoutResults(workoutProgress.workout.id, exercises = exercises)
            state.value = state.value.copy(
                workoutResults = workoutResults,
                isWaitingToHitTractionGoal = false,
            )
            return workoutResults
        }

        val workoutResults = currentWorkoutResults.copyWithAddedResults(
            exerciseResults = exerciseResults,
            workoutProgress = workoutProgress
        )
        state.value = state.value.copy(
            workoutResults = workoutResults
        )
        return workoutResults
    }

    private suspend fun storeResults(workoutResults: WorkoutResults) {
        // TODO: store results if possible move to repository
        delay(10)
    }
}

fun  WorkoutResults.lastSetResult(): SetResult {
    return exercises.values.last().values.last()
}

fun WorkoutResults.copyWithAddedResults(exerciseResults: Map<Int, SetResult>, workoutProgress: WorkoutProgress): WorkoutResults {
    // TODO: unit tests
    val exercise = workoutProgress.activeExercise()

    val currentExerciseResults = exercises[exercise.name]
    // no results for this exercise yet? create exercise with first set and copy other exercises
    if (currentExerciseResults == null) {
        val newExercises = mutableMapOf(exercise.name to exerciseResults)
        exercises.toMap(newExercises)

        return this.copy(exercises = newExercises)
    }

    // copy exercises
    val newExercises = mutableMapOf<String, Map<Int, SetResult>>()
    exercises.toMap(newExercises)

    // copy sets
    val newExerciseResults = exerciseResults.toMutableMap()
    currentExerciseResults.toMap(newExerciseResults)

    // override current exercise
    newExercises[exercise.name] = newExerciseResults
    return this.copy(exercises = newExercises)
}