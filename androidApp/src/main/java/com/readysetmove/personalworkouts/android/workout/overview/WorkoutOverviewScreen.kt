package com.readysetmove.personalworkouts.android.workout.overview.overview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.Exercise
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.Workout
import com.readysetmove.personalworkouts.android.components.WorkoutOverviewCard
import com.readysetmove.personalworkouts.android.theme.AppTheme


@Composable
fun WorkoutOverviewScreen(workout: Workout) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .verticalScroll(scrollState)
        .padding(AppTheme.spacings.md)) {
        WorkoutOverviewCard(stringResource(R.string.workout_overview__todays_workout_title),
            workout = workout)
    }
}

@Preview(name = "Light Mode", widthDp = 1024)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 1024
)
@Composable
fun PreviewWorkoutOverviewCard() {
    AppTheme {
        WorkoutOverviewScreen(Workout(exercises = listOf(Exercise(name = "Rows", "Rows Cmt"),
            Exercise(name = "Front Press", "Press Cmt"),
            Exercise(name = "Deadlift", "DL Cmt"), Exercise(name = "Squats", "Squats Cmt")),
            "Wkt Cmt"))
    }
}