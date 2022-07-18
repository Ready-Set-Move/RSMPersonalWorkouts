package com.readysetmove.personalworkouts.android.workout

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.readysetmove.personalworkouts.app.AppStore
import com.readysetmove.personalworkouts.device.IsDeviceStore
import com.readysetmove.personalworkouts.workout.*
import org.koin.androidx.compose.get

object WorkoutScreen {
    const val ROUTE = "workout"
}

@Composable
fun WorkoutScreen(
    appStore: AppStore = get(),
    workoutStore: WorkoutStore = get(),
    deviceStore: IsDeviceStore = get(),
    onNavigateBack: () -> Unit
) {
    val appState = appStore.observeState().collectAsState()
    val workoutState = workoutStore.observeState().collectAsState()
    val deviceState = deviceStore.observeState().collectAsState()

    // TODO: we need more screens and states for exercise start and finish
    workoutState.value.let { state ->
        when(state) {
            is WorkoutState.NoWorkout -> Text(text = "No workout started")
            // TODO: Add exercise waiting layer to flow and auto start next set
            is WorkoutState.WaitingToStartExercise ->
                ExerciseOverviewScreen(
                    exercise = state.workoutProgress.activeExercise(),
                    onNavigateBack = onNavigateBack
                ) {
                workoutStore.dispatch(state.startExerciseAction())
            }
            is WorkoutState.WaitingToStartSet, is WorkoutState.Working ->
                (state as IsExercisingState).apply {
                    WorkingScreen(
                        tractionGoal = (state.tractionGoal/1000).toInt(),
                        timeToWork = state.timeLeft,
                        currentLoad = deviceState.value.traction,
                    )
                }
            // TODO: merge resting with showing results and rating
            is WorkoutState.Resting ->
                RestingScreen(timeToRest = state.timeLeft)
            is WorkoutState.SetFinished ->
                SetResultsScreen(
                    exercise = state.workoutProgress.activeExercise(),
                    set = state.workoutProgress.activeSet(),
                    latestTractions = appState.value.latestSetResult?.tractions,
                    setIndex = state.workoutProgress.activeSetIndex,
                ) {
                    workoutStore.dispatch(state.rateSetAction(rating = 1))
                }
            is WorkoutState.ExerciseFinished ->
                ExerciseResultsScreen(exercise = state.workoutProgress.activeExercise()) {
                    workoutStore.dispatch(state.rateExerciseAction(rating = 1))
                }
            is WorkoutState.WorkoutFinished ->
                WorkoutFinishedScreen()
        }
    }
}
