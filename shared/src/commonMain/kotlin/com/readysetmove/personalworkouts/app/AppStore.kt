package com.readysetmove.personalworkouts.app

import com.readysetmove.personalworkouts.device.DeviceAction
import com.readysetmove.personalworkouts.device.DeviceStore
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

data class WorkoutResults(val workoutId: String, val exercises: Map<String, Map<Int, List<Float>>>)

data class AppState(
    val workout: Workout? = null,
    val workoutResults: WorkoutResults? = null
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
    private val deviceStore: DeviceStore,
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
            // TODO: here we need to fetch the workout
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
                    deviceStore.observeState().first { deviceState ->
                        // ensure we have a valid workout state
                        val workoutState = workoutStore.observeState().first { workoutState ->
                            workoutState.timeToWork > 0 && workoutState.tractionGoal != null
                        }
                        deviceState.traction >= workoutState.tractionGoal!!
                    }
                    startTracking()
                }
            }
        }
    }

    private suspend fun startTracking() {
        deviceStore.dispatch(DeviceAction.StartTracking)
        workoutStore.observeSideEffect()
            .filterIsInstance<WorkoutSideEffect.SetFinished>()
            .first { setFinishedEffect ->
                deviceStore.dispatch(DeviceAction.StopTracking)
                val stoppedState = deviceStore.observeState().first { deviceState -> !deviceState.trackingActive }
                val workoutResults = updateStateWithResults(
                    tractions = stoppedState.trackedTraction,
                    workoutProgress = setFinishedEffect.workoutProgress
                )
                storeResults(workoutResults = workoutResults)
                true
            }
    }

    private fun updateStateWithResults(tractions: List<Float>, workoutProgress: WorkoutProgress): WorkoutResults {
        val exercise = workoutProgress.workout.exercises[workoutProgress.activeExercise]
        val currentWorkoutResults = state.value.workoutResults
        // no results yet? create everything from scratch
        if (currentWorkoutResults == null) {
            val exerciseResults = mapOf(workoutProgress.activeSet to tractions)
            val exercises = mapOf(exercise.name to exerciseResults)
            val workoutResults = WorkoutResults(workoutProgress.workout.id, exercises = exercises)
            state.value = state.value.copy(
                workoutResults = workoutResults
            )
            return workoutResults
        }

        val workoutResults = currentWorkoutResults.copyWithAddedResults(
            tractions = tractions,
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

fun WorkoutResults.copyWithAddedResults(tractions: List<Float>, workoutProgress: WorkoutProgress): WorkoutResults {
    // TODO: unit tests
    val exercise = workoutProgress.workout.exercises[workoutProgress.activeExercise]

    val currentExerciseResults = exercises[exercise.name]
    // no results for this exercise yet? create exercise with first set and copy other exercises
    if (currentExerciseResults == null) {
        val exerciseResults = mapOf(workoutProgress.activeSet to tractions)
        val newExercises = mutableMapOf(exercise.name to exerciseResults)
        exercises.toMap(newExercises)

        return this.copy(exercises = newExercises)
    }

    // copy exercises
    val newExercises = mutableMapOf<String, Map<Int, List<Float>>>()
    exercises.toMap(newExercises)

    // copy sets
    val exerciseResults = mutableMapOf(workoutProgress.activeSet to tractions)
    currentExerciseResults.toMap(exerciseResults)

    // override current exercise
    newExercises[exercise.name] = exerciseResults
    return this.copy(exercises = newExercises)
}