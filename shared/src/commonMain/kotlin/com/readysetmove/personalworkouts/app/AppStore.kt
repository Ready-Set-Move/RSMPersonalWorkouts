package com.readysetmove.personalworkouts.app

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.IsWorkoutRepository
import com.readysetmove.personalworkouts.workout.Workout
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class AppState(
    val user: User? = null,
    val workout: Workout? = null,
) : State

sealed class AppAction: Action {
    data class SetUser(val user: User): AppAction()
    object UnsetUser: AppAction()
}

sealed class AppSideEffect : Effect

// TODO: implement SettingsStore
// TODO: implement UserStore to handle Firebase logic
class AppStore(
    initialState: AppState = AppState(),
    private val workoutRepository: IsWorkoutRepository,
    private val mainDispatcher: CoroutineContext,
):
    Store<AppState, AppAction, AppSideEffect>,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(initialState)
    private val sideEffect = MutableSharedFlow<AppSideEffect>()
    override fun observeState(): StateFlow<AppState> = state
    override fun observeSideEffect(): Flow<AppSideEffect> = sideEffect

    private val tag = "AppStore"

    init {
//        launch {
//            workoutRepository.saveWorkout(
//                userId = "CoHbkbOvIUYg4y2NSheGQFtiV2P2",
//                workout = WorkoutBuilder.workout {
//                    exercise("Overhead Press", position = "?") {
//                        assessmentTest(5, 10, 15)
//                    }
//                    exercise("Squat", position = "@home: 5 | @studio: holds direct") {
//                        assessmentTest(45, 60, 95)
//                    }
//                    exercise("Shrugs", position = "@home: 6 | @studio: holds direct or 0") {
//                        assessmentTest(40, 60, 85)
//                    }
//                    exercise("Drag Curls", position = "?") {
//                        assessmentTest(5, 10, 15)
//                    }
//                }
//            )
//        }
        state.value.user?.let {
            fetchWorkoutForUser(it)
        }
    }

    private fun fetchWorkoutForUser(user: User) {
        launch {
            Napier.d("Starting side effect to fetch workout for user id: ${user.id}", tag = tag)
            // TODO: error handling: reset userId and throw error
//            val workout = workoutRepository.fetchLatestWorkoutForUser("6QpQhtAwRZVd7CsIQeakb2i3V9k1")
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
        }
    }
}