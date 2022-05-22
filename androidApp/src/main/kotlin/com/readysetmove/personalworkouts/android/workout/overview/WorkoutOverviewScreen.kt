package com.readysetmove.personalworkouts.android.workout.overview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.workout.Mocks
import com.readysetmove.personalworkouts.workout.Workout


object WorkoutOverviewScreen {
    const val ROUTE = "workout-overview"
}

@Composable
fun WorkoutOverviewScreen(workout: Workout?, onStartWorkout: () -> Unit) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.workout_overview__screen_title)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                Modifier.semantics { contentDescription = title }
            )
        },
        floatingActionButton = {
            val actionText = stringResource(R.string.workout_overview__start_workout)
            ExtendedFloatingActionButton(
                icon = {
                    Icon(imageVector = Icons.Filled.PlayCircle,
                        contentDescription = actionText)
                },
                text = { Text(text = actionText) },
                onClick = onStartWorkout,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .verticalScroll(scrollState)
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)) {
            if (workout == null) return@Column Text(text = "No workout set")

            WorkoutOverviewCard(stringResource(R.string.workout_overview__todays_workout_title),
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
            workout = Mocks.workout,
            onStartWorkout = {}
        )
    }
}