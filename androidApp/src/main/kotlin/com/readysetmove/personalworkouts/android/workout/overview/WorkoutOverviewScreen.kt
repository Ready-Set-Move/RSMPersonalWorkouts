package com.readysetmove.personalworkouts.android.workout.overview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.workout.*
import com.readysetmove.personalworkouts.workout.Set
import org.koin.androidx.compose.get


object WorkoutOverviewScreen {
    const val ROUTE = "workout-overview"
}

@Composable
fun WorkoutOverviewScreen(userName: String, workout: Workout?, workoutStore: WorkoutStore = get(), onNavigateBack: () -> Unit) {
    val workoutState = workoutStore.observeState().collectAsState()
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.workout_overview__screen_title)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigation__back))
                    }
                },
                modifier = Modifier.semantics { contentDescription = title }
            )
        },
        floatingActionButton = {
            // TODO: split into a splash screen higher up in the tree
            workout?.let { workout ->
                workoutState.value.let { currentState ->
                    // TODO: here we could handle aborting running workout
                    if (currentState is WorkoutState.NoWorkout) {
                        val actionText = stringResource(R.string.workout_overview__start_workout)
                        ExtendedFloatingActionButton(
                            icon = {
                                Icon(imageVector = Icons.Filled.PlayCircle,
                                    contentDescription = actionText)
                            },
                            text = { Text(text = actionText) },
                            onClick = {
                                workoutStore.dispatch(currentState.startWorkoutAction(workout = workout))
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .verticalScroll(scrollState)
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)) {
            if (workout == null) return@Column Text(text = "No workout set")

            WorkoutOverviewCard("Hi $userName",
                workout = workout)
        }
    }
}

@Preview(name = "Light Mode", widthDp = 320)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 1024
)
@Composable
fun PreviewWorkoutOverviewScreen() {
    AppTheme {
        WorkoutOverviewScreen(
            workout = WorkoutBuilder.workout {
                exercise("Shrugs", position = "7") {
                    warmup(xMin = 30, min = 50, med = 75, max = 100)
                    set(Set(100), repeat = 6)
                }
                exercise("Calf Lifts", position = "7") {
                    warmup(min = 30, med = 45, max = 65)
                    set(Set(65, duration = 15), repeat = 1)
                    set(Set(65, duration = 12), repeat = 3)
                }
            },
            userName = "Bob"
        ) {}
    }
}