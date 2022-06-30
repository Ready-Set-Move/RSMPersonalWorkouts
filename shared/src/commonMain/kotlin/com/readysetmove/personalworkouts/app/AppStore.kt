package com.readysetmove.personalworkouts.app

import com.readysetmove.personalworkouts.device.DeviceAction
import com.readysetmove.personalworkouts.device.IsDeviceStore
import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.*
import com.readysetmove.personalworkouts.workout.results.IsWorkoutResultsRepository
import com.readysetmove.personalworkouts.workout.results.SetResult
import com.readysetmove.personalworkouts.workout.results.WorkoutResults
import com.readysetmove.personalworkouts.workout.results.copyWithAddedResults
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

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
    private val workoutResultsRepository: IsWorkoutResultsRepository,
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
        state.value.user?.let {
            fetchWorkoutForUser(it)
        }
    }

    private fun fetchWorkoutForUser(user: User) {
        launch {
            Napier.d("Starting side effect to fetch workout for user id: ${user.id}", tag = tag)
            // TODO: error handling: reset userId and throw error
            val workout = workoutRepository.fetchLatestWorkoutForUser(user.id)
            // still the same user set?
            if (state.value.user == user) {
                Napier.d("Updating state with new workout $workout for user: $user", tag = tag)
                state.value = state.value.copy(workout = workout)
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
                Napier.d("New user received: $action.user updating state", tag = tag)
                fetchWorkoutForUser(action.user)
                state.value = AppState(user = action.user)
            }
            is AppAction.StartWorkout -> {
                state.value.workout?.let { workout ->
                    workoutStore.dispatch(WorkoutAction.StartWorkout(workout))
                } ?: launch {
                        sideEffect.emit(AppSideEffect.NoWorkoutSet)
                }
            }
            is AppAction.StartNextSet -> {
                when(state.value.workout) {
                    null -> launch {
                        sideEffect.emit(AppSideEffect.NoWorkoutSet)
                    }
                    else -> {
                        workoutState?.workoutProgress?.activeSet()?.tractionGoal?.let { currentTractionGoal ->
                            listenForSetStart(currentTractionGoal)
                            // TODO: pull up tracking! Separate into WorkoutResultsStore?
                            startTracking()
                            state.value = state.value.copy(isWaitingToHitTractionGoal = true)
                        } ?: launch {
                            sideEffect.emit(AppSideEffect.NoSetInProgress)
                        }
                    }
                }
            }
        }
    }

    private fun listenForSetStart(currentTractionGoal: Long) {
        launch {
            // handle max sets with 0 weight starting at 5
            val tractionGoalThreshold = if(currentTractionGoal>0) currentTractionGoal*.8f else 5f
            // start tracking as soon as we have relevant traction delivered
            deviceStore.observeState().first { deviceState ->
                // start countdown and tracking as soon as we hit relevant weight
                // TODO: what if we don't hit the minimal goal?
                deviceState.traction*1000 >= tractionGoalThreshold
            }
            workoutStore.dispatch(WorkoutAction.StartSet)
        }
    }
    private fun startTracking() {
        launch {
            workoutStore.observeSideEffect()
                .filterIsInstance<WorkoutSideEffect.SetFinished>()
                .first { setFinishedEffect ->
                    deviceStore.dispatch(DeviceAction.StopTracking)
                    val stoppedState = deviceStore.observeState().first { deviceState -> !deviceState.trackingActive }

                    state.value = state.value.updateWithResults(
                        tractions = stoppedState.trackedTraction,
                        workoutProgress = setFinishedEffect.workoutProgress,
                        tractionGoal = setFinishedEffect.tractionGoal,
                    ) {
                        launch {
                            workoutResultsRepository.storeResults(it)
                        }
                    }
                    true
                }
        }
        deviceStore.dispatch(DeviceAction.StartTracking)
    }
}

fun AppState.updateWithResults(
    tractions: List<Traction>,
    tractionGoal: Long,
    workoutProgress: WorkoutProgress,
    onResultsUpdated: (WorkoutResults) -> Unit
): AppState {
    // only update results if workout is still running
    if (workout == null) return this

    // TODO: segment tracked data in ramp up < work > ramp down phases
    // tractions need timestamps relative to beginning of first traction
    val setStart = tractions.first().timestamp
    val setResult = SetResult(
        tractionGoal = tractionGoal,
        tractions = tractions.map {
            it.copy(timestamp = it.timestamp - setStart)
        }
    )

    val workoutResults = workoutResults.copyWithAddedResults(
        setWithResult = workoutProgress.activeSetIndex to setResult,
        exerciseName = workoutProgress.activeExercise().name,
        workoutId = workoutProgress.workout.id,
    )

    onResultsUpdated(workoutResults)

    return copy(
        workoutResults = workoutResults,
        isWaitingToHitTractionGoal = false,
        latestSetResult = setResult,
    )
}