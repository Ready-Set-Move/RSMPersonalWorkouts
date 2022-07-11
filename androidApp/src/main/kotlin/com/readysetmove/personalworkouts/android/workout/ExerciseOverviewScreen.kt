package com.readysetmove.personalworkouts.android.workout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.workout.Exercise
import com.readysetmove.personalworkouts.workout.ExerciseBuilder

@Composable
fun ExerciseOverviewScreen(
    exercise: Exercise,
    onNavigateBack: () -> Unit,
    onStartExercise: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.workout_exercise_overview__title)
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
        },
        floatingActionButton = {
            val actionText = stringResource(R.string.workout_exercise_overview__start)
            ExtendedFloatingActionButton(
                icon = {
                    Icon(imageVector = Icons.Filled.PlayCircle,
                        contentDescription = actionText)
                },
                text = { Text(text = actionText) },
                onClick = onStartExercise,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)
            .verticalScroll(scrollState))
        {
            Text(text = exercise.name, style = AppTheme.typography.h3)
            Text(text = "Position: ${exercise.position}", style = AppTheme.typography.body1)
            exercise.sets.mapIndexed { index, set ->
                Text(text = "Set #$index", style = AppTheme.typography.h6)
                Row(modifier = Modifier.padding(AppTheme.spacings.sm)) {
                    Text(text = "${set.tractionGoal} kg", style = AppTheme.typography.body1)
                    Text(text = " | ${set.duration} s", style = AppTheme.typography.body1)
                    Text(text = " | ${set.restTime} s rest", style = AppTheme.typography.body1)
                }
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
fun ExerciseOverviewPreview() {
    AppTheme {
        ExerciseOverviewScreen(
            exercise = ExerciseBuilder.exercise {
                assessmentTest(min = 50, med = 75, max = 100)
            },
            onStartExercise = {},
            onNavigateBack = {},
        )
    }
}