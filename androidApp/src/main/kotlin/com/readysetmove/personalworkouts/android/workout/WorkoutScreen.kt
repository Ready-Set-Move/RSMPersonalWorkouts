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
import com.readysetmove.personalworkouts.workout.EntityMocks
import com.readysetmove.personalworkouts.workout.Exercise
import com.readysetmove.personalworkouts.workout.Set

object WorkoutScreen {
    const val ROUTE = "workout"
}

@Composable
fun WorkoutScreen(exercise: Exercise, set: Set, timeToWork: Long, timeToRest: Long, onStartSet: () -> Unit, onNavigateBack: () -> Unit) {
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
            Text(text = exercise.name)
            Text(text = "Weight: ${set.tractionGoal/1000} kg")
            Text(text = "Work: ${if (timeToWork > 1000) timeToWork/1000 else "%.1f".format((timeToWork).toFloat()/1000)} s")
            Text(text = "Rest: ${if (timeToRest > 1000) timeToRest/1000 else "%.1f ".format((timeToRest).toFloat()/1000)} s")
            Button(onClick = onStartSet) {
                Text(text = "Start Set")
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
            exercise = EntityMocks.ROWS,
            set = EntityMocks.SET,
            timeToWork = 6000,
            timeToRest = 0,
            onStartSet = {},
            onNavigateBack = {}
        )
    }
}