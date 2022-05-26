package com.readysetmove.personalworkouts.app

import com.readysetmove.personalworkouts.device.DeviceStore
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.EntityMocks
import com.readysetmove.personalworkouts.workout.Workout
import com.readysetmove.personalworkouts.workout.WorkoutAction
import com.readysetmove.personalworkouts.workout.WorkoutStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class AppState(val workout: Workout? = null) : State

sealed class AppAction: Action {
    object StartWorkout: AppAction()
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
        launch {
            workoutStore.observeState().collect {
            }
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
        }
    }
}
