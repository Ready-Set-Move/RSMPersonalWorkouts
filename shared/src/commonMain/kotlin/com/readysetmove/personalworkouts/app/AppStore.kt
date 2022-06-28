package com.readysetmove.personalworkouts.app

import com.readysetmove.personalworkouts.device.DeviceAction
import com.readysetmove.personalworkouts.device.IsDeviceStore
import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class SetResult(val tractionGoal: Long, val tractions: List<Traction>)

data class WorkoutResults(val workoutId: String, val exercises: Map<String, Map<Int, SetResult>>)

data class AppState(
    val user: User? = null,
    val workout: Workout? = null,
    val workoutResults: WorkoutResults? = null,
    val isWaitingToHitTractionGoal: Boolean = false,
    val latestSetResult: SetResult? = null,
) : State

sealed class AppAction: Action {
    data class SetUser(val user: User): AppAction()
    object UnsetUser: AppAction()
    object StartWorkout: AppAction()
    object StartNextSet: AppAction()
}

sealed class AppSideEffect : Effect {
    object NoWorkoutSet : AppSideEffect()
    object NoSetInProgress : AppSideEffect()
}

class AppStore(
    initialState: AppState = AppState(),
    private val workoutRepository: IsWorkoutRepository,
    private val deviceStore: IsDeviceStore,
    private val workoutStore: WorkoutStore,
    private val mainDispatcher: CoroutineContext,
):
    Store<AppState, AppAction, AppSideEffect>,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(initialState)
    private val sideEffect = MutableSharedFlow<AppSideEffect>()
    override fun observeState(): StateFlow<AppState> = state
    override fun observeSideEffect(): Flow<AppSideEffect> = sideEffect

    private val tag = "AppStore"

    private var workoutState: WorkoutState? = null

    init {
        launch {
            workoutStore.observeState().collect {
                workoutState = it
            }
        }
    }

    override fun dispatch(action: AppAction) {
        when(action) {
            is AppAction.UnsetUser -> {
                Napier.d("Unsetting user", tag = tag)
                if (state.value.user == null) return
                // complete reset
                state.value = AppState()
            }
            is AppAction.SetUser -> {
                if (state.value.user == action.user) return

                Napier.d("New user received: ${action.user} updating state", tag = tag)
                launch {
                    Napier.d("Starting side effect to fetch workout for user id: ${action.user.id}", tag = tag)
                    // TODO: error handling: reset userId and throw error
                    val workout = workoutRepository.fetchLatestWorkoutForUser(action.user.id)
                    // still the same user set?
                    if (state.value.user == action.user) {
                        Napier.d("Updating state with new workout $workout for user: ${action.user}", tag = tag)
                        state.value = state.value.copy(workout = workout)
                    }
                }
                state.value = AppState(user = action.user)
            }
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
                if (state.value.workout == null) {
                    launch {
                        sideEffect.emit(AppSideEffect.NoWorkoutSet)
                    }
                    return
                }
                val currentTractionGoal = workoutState?.workoutProgress?.activeSet()?.tractionGoal
                if (currentTractionGoal == null) {
                    launch {
                        sideEffect.emit(AppSideEffect.NoSetInProgress)
                    }
                    return
                }
                launch {
                    // handle max sets with 0 weight starting at 5
                    val tractionGoalThreshold = if(currentTractionGoal>0f) currentTractionGoal*.8f else 5f
                    // start tracking as soon as we have relevant traction delivered
                    deviceStore.observeState().first { deviceState ->
                        // start countdown and tracking as soon as we hit relevant weight
                        // TODO: what if we don't hit the minimal goal?
                        deviceState.traction*1000 >= tractionGoalThreshold
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
                    // don't update results if workout is no longer running
                    if (state.value.workout != null) {
                        val workoutResults = updateStateWithResults(
                            tractions = stoppedState.trackedTraction,
                            workoutProgress = setFinishedEffect.workoutProgress,
                            tractionGoal = setFinishedEffect.tractionGoal,
                        )
                        storeResults(workoutResults = workoutResults)
                    }
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
        val setResult = SetResult(
            tractionGoal = tractionGoal,
            tractions = timedTractions
        )
        val exerciseResults = mapOf(workoutProgress.activeSetIndex to setResult)
        // no results yet? create everything from scratch
        if (currentWorkoutResults == null) {
            val exercises = mapOf(exercise.name to exerciseResults)
            val workoutResults = WorkoutResults(workoutProgress.workout.id, exercises = exercises)
            state.value = state.value.copy(
                workoutResults = workoutResults,
                isWaitingToHitTractionGoal = false,
                latestSetResult = setResult,
            )
            return workoutResults
        }

        val workoutResults = currentWorkoutResults.copyWithAddedResults(
            exerciseResults = exerciseResults,
            workoutProgress = workoutProgress
        )
        state.value = state.value.copy(
            workoutResults = workoutResults,
            isWaitingToHitTractionGoal = false,
            latestSetResult = setResult,
        )
        return workoutResults
    }

    private suspend fun storeResults(workoutResults: WorkoutResults) {
        // TODO: store results if possible move to repository
        delay(10)
    }
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