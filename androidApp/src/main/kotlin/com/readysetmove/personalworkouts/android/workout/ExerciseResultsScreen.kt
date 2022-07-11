package com.readysetmove.personalworkouts.android.workout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.readysetmove.personalworkouts.workout.Exercise
import com.readysetmove.personalworkouts.workout.ExerciseBuilder

@Composable
fun ExerciseResultsScreen(
    exercise: Exercise,
    onProceed: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.workout_exercise_results__title)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                modifier = Modifier.semantics { contentDescription = title }
            )
        },
        floatingActionButton = {
            val actionText = stringResource(R.string.workout_exercise_results__rate)
            ExtendedFloatingActionButton(
                icon = {
                    Icon(imageVector = Icons.Filled.PlayCircle,
                        contentDescription = actionText)
                },
                text = { Text(text = actionText) },
                onClick = onProceed,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)
            .verticalScroll(scrollState)
        ) {
            Text(
                text = "${exercise.name} DONE",
                style = AppTheme.typography.h1,
            )
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
fun ExerciseResultsScreenPreview() {
    val exercise = ExerciseBuilder.exercise(name = "Rows") {
        assessmentTest(25, 50, 75)
    }
    AppTheme {
        ExerciseResultsScreen(
            exercise = exercise,
        ) {}
    }
}