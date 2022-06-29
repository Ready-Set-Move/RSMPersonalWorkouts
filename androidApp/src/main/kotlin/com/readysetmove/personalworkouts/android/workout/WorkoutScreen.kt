package com.readysetmove.personalworkouts.android.workout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.workout.Exercise
import com.readysetmove.personalworkouts.workout.ExerciseBuilder
import com.readysetmove.personalworkouts.workout.Set

object WorkoutScreen {
    const val ROUTE = "workout"
}

@Composable
fun WorkoutScreen(
    exercise: Exercise,
    set: Set,
    timeToWork: Long,
    timeToRest: Long,
    currentLoad: Float = 0f,
    setInProgress: Boolean = false,
    latestTractions: List<Traction>?,
    onStartSet: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.workout__screen_title)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigation__back))
                    }
                },
                modifier = Modifier.semantics { contentDescription = title }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .verticalScroll(scrollState)
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)) {
            Text(text = exercise.name, style = AppTheme.typography.h3)
            Text(text = "Goal: ${set.tractionGoal/1000} kg")
            Text(text = "Position: %.1f".format(exercise.position))
            Text(text = "%.1f".format(currentLoad-set.tractionGoal/1000), style = AppTheme.typography.h1)
            Text(text = "${if (timeToWork > 1000) timeToWork/1000 else "%.1f".format((timeToWork).toFloat()/1000)}s", style = AppTheme.typography.h1)
            Text(text = "Rest: ${if (timeToRest > 1000) timeToRest/1000 else "%.1f ".format((timeToRest).toFloat()/1000)} s")
            Button(onClick = onStartSet, enabled = !setInProgress) {
                Text(text = "Start Set")
            }
            Text(text = "Max: %.1f".format(latestTractions?.maxByOrNull { it.value }?.value ?: 0.0))
            latestTractions?.map {
                Text(text = "%.1f".format(it.value))
            }
        }
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
fun PreviewWorkoutScreen() {
    AppTheme {
        WorkoutScreen(
            exercise = ExerciseBuilder(name = "Rows", comment = "Rows Cmt", position = 6.5f).build(),
            set = Set(tractionGoal = 50000, duration = 6000, restTime = 5000),
            timeToWork = 6000,
            timeToRest = 0,
            onStartSet = {},
            onNavigateBack = {},
            latestTractions = emptyList()
        )
    }
}