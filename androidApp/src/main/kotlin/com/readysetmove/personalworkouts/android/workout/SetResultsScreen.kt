package com.readysetmove.personalworkouts.android.workout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.workout.Exercise
import com.readysetmove.personalworkouts.workout.ExerciseBuilder
import com.readysetmove.personalworkouts.workout.Set

@Composable
fun SetResultsScreen(
    exercise: Exercise,
    set: Set,
    latestTractions: List<Traction>?,
    setIndex: Int,
    onProceed: () -> Unit,
) {
    val title = stringResource(R.string.workout_set_results__title)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                modifier = Modifier.semantics { contentDescription = title }
            )
        },
        floatingActionButton = {
            val actionText = stringResource(R.string.workout_set_results__rate)
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
        ) {
            Text(
                text = "${exercise.name}: ${setIndex + 1} / ${exercise.sets.size}",
                style = AppTheme.typography.h4
            )
            Text(text = "Goal: ${set.tractionGoal} kg", style = AppTheme.typography.h4)
            Text(
                text = "Max: %.1f".format(latestTractions?.maxByOrNull { it.value }?.value ?: 0.0),
                style = AppTheme.typography.body1
            )
            latestTractions?.let { tractions ->
                LazyColumn {
                    items(tractions) { traction ->
                        Text(text = "%.1f @ %.1f".format(traction.value, traction.timestamp/1000f))
                    }
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
fun SetResultsScreenPreview() {
    val exercise = ExerciseBuilder.exercise(name = "Rows") {
        assessmentTest(25, 50, 75)
    }
    val  setIndex = 3
    AppTheme {
        SetResultsScreen(
            exercise = exercise,
            set = exercise.sets[setIndex],
            latestTractions = listOf(
                Traction(value = 5f, timestamp = 1000),
                Traction(value = 8f, timestamp = 2000),
                Traction(value = 12f, timestamp = 3000),
                Traction(value = 500f, timestamp = 5000),
                Traction(value = 50f, timestamp = 5200),
                Traction(value = 5f, timestamp = 6666),
            ),
            setIndex = setIndex,
        ) {}
    }
}