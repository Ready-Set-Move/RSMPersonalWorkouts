package com.readysetmove.personalworkouts.android.workout

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.readysetmove.personalworkouts.device.IsDeviceStore
import com.readysetmove.personalworkouts.workout.progress.*
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsStore
import org.koin.androidx.compose.get

object WorkoutScreen {
    const val ROUTE = "workout"
}

@Composable
fun WorkoutScreen(
    workoutProgressStore: WorkoutProgressStore = get(),
    workoutResultsStore: WorkoutResultsStore = get(),
    deviceStore: IsDeviceStore = get(),
    onNavigateBack: () -> Unit
) {
    val workoutState = workoutProgressStore.observeState().collectAsState()
    val workoutResultsState = workoutResultsStore.observeState().collectAsState()
    val deviceState = deviceStore.observeState().collectAsState()

    workoutState.value.let { state ->
        when(state) {
            is WorkoutProgressState.NoWorkout -> Text(text = "No workout started")
            is WorkoutProgressState.WaitingToStartExercise ->
                ExerciseOverviewScreen(
                    exercise = state.workoutProgress.activeExercise(),
                    onNavigateBack = onNavigateBack
                ) {
                workoutProgressStore.dispatch(state.startExerciseAction())
            }
            is WorkoutProgressState.WaitingToStartSet, is WorkoutProgressState.Working ->
                (state as IsExercisingState).apply {
                    WorkingScreen(
                        tractionGoal = (state.tractionGoal/1000).toInt(),
                        timeToWork = state.timeLeft,
                        currentLoad = deviceState.value.traction,
                    )
                }
            // TODO: merge resting with showing results and rating
            is WorkoutProgressState.Resting, is WorkoutProgressState.SetFinished ->
                SetResultsScreen(
                    workoutProgressState = state as IsExercisingState,
                    workoutResultsState = workoutResultsState.value,
                ) { rateSetAction, goToNextSetAction ->
                    workoutResultsStore.dispatch(rateSetAction)
                    workoutProgressStore.dispatch(goToNextSetAction)
                }
            is WorkoutProgressState.ExerciseFinished ->
                ExerciseResultsScreen(exercise = state.workoutProgress.activeExercise()) {
                    workoutProgressStore.dispatch(state.goToNextExerciseAction())
                }
            is WorkoutProgressState.WorkoutFinished ->
                WorkoutFinishedScreen()
        }
    }
}
