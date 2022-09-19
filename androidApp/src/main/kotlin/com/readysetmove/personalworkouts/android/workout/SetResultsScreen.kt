package com.readysetmove.personalworkouts.android.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.android.workout.results.TractionsGraph
import com.readysetmove.personalworkouts.device.getMedianTraction
import com.readysetmove.personalworkouts.workout.progress.*
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsAction
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsState
import com.readysetmove.personalworkouts.workout.results.rateSetAction

@Composable
fun SetResultsScreen(
    workoutProgressState: IsExercisingState,
    workoutResultsState: WorkoutResultsState,
    onProceed: (
        rateSetAction: WorkoutResultsAction.RateSet,
        goToNextSetAction: WorkoutProgressAction.GoToNextSet
    ) -> Unit,
) {
    val title = stringResource(R.string.workout_set_results__title)
    val exercise = workoutProgressState.workoutProgress.activeExercise()
    val set = workoutProgressState.workoutProgress.activeSet()
    val setIndex = workoutProgressState.workoutProgress.activeSetIndex

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                modifier = Modifier.semantics { contentDescription = title }
            )
        },
        // TODO: this will be a real rating component later on
        floatingActionButton = {
            if (workoutResultsState !is WorkoutResultsState.WaitingToRateSet) return@Scaffold
            if (workoutProgressState !is WorkoutProgressState.SetFinished) return@Scaffold
            val actionText = stringResource(R.string.workout_set_results__rate)
            ExtendedFloatingActionButton(
                icon = {
                    Icon(imageVector = Icons.Filled.PlayCircle,
                        contentDescription = actionText)
                },
                text = { Text(text = actionText) },
                onClick = {
                    onProceed(
                        workoutResultsState.rateSetAction(
                            tractionGoal = workoutProgressState.tractionGoal,
                            durationGoal = workoutProgressState.durationGoal,
                            workoutProgress = workoutProgressState.workoutProgress,
                            rating = 1,
                        ),
                        workoutProgressState.goToNextSetAction()
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)
        ) {
            Text(
                text = "${exercise.name}: ${setIndex + 1} / ${exercise.sets.size}",
                style = AppTheme.typography.h4
            )
            Text(text = "Goal: ${set.tractionGoal} kg", style = AppTheme.typography.h4)
            if (workoutResultsState is WorkoutResultsState.WaitingToRateSet) {
                Text(
                    text = "Max: %.1f".format(workoutResultsState.tractions.maxByOrNull { it.value }?.value ?: 0.0),
                    style = AppTheme.typography.body1
                )
                Text(
                    text = "Median: %.1f".format(workoutResultsState.tractions.getMedianTraction()),
                    style = AppTheme.typography.body1
                )
                TractionsGraph(
                    tractions = workoutResultsState.tractions,
                    tractionGoal = set.tractionGoal ,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dp(350f)),
                )
                Row(Modifier.height(Dp(200f))) {
                    workoutResultsState.tractions.let { tractions ->
                        LazyColumn {
                            items(tractions.reversed()) { traction ->
                                Text(text = "%.1f @ %.1f".format(traction.value, traction.timestamp/1000f))
                            }
                        }
                    }
                }
            }
            if (workoutProgressState is WorkoutProgressState.Resting) {
                RestingScreen(timeToRest = workoutProgressState.timeLeft)
            }
        }
    }
}


// TODO: implement or split screen from simple UI
//@Preview(name = "Light Mode", widthDp = 1024)
//@Preview(
//    name = "Dark Mode",
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    widthDp = 1024
//)
//@Composable
//fun SetResultsScreenPreview() {
//    val exercise = ExerciseBuilder.exercise(name = "Rows") {
//        assessmentTest(25, 50, 75)
//    }
//    val  setIndex = 3
//    AppTheme {
//        SetResultsScreen(
//            workoutProgressState = object : IsExercisingState {
//
//                                                              },
//            exercise = exercise,
//            set = exercise.sets[setIndex],
//            latestTractions = listOf(
//                Traction(value = 5f, timestamp = 1000),
//                Traction(value = 8f, timestamp = 2000),
//                Traction(value = 12f, timestamp = 3000),
//                Traction(value = 500f, timestamp = 5000),
//                Traction(value = 50f, timestamp = 5200),
//                Traction(value = 5f, timestamp = 6666),
//            ),
//            setIndex = setIndex,
//        ) {}
//    }
//}