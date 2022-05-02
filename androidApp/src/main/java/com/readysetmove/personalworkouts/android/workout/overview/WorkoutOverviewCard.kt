package com.readysetmove.personalworkouts.android.workout.overview

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.Exercise
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.Workout
import com.readysetmove.personalworkouts.android.components.ExpandableContent
import com.readysetmove.personalworkouts.android.theme.AppTheme

@Composable
fun WorkoutOverviewCard(title: String, workout: Workout) {
    var expanded by rememberSaveable {
        mutableStateOf(true)
    }
    Card(
        onClick = { expanded = !expanded },
        onClickLabel = if (expanded) "Hide detailed workout information" else "Show detailed workout information"
    )
    {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacings.md)) {
            ExpandableContent(additionalContent = {
                Spacer(modifier = Modifier.height(AppTheme.spacings.sm))
                Text(
                    text = workout.comment,
                    style = AppTheme.typography.body1,
                )
            },
                expanded = expanded) {
                Box {
                    Text(text = title, style = AppTheme.typography.h1)
                    Icon(modifier = Modifier.align(Alignment.CenterEnd),
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(
                            id = if (expanded) R.string.expandable_card__less else R.string.expandable_card__more))
                }
            }
            Spacer(modifier = Modifier.height(AppTheme.spacings.md))
            Column {
                workout.exercises.map {
                    WorkoutRow(exercise = it, expanded = expanded)
                    Spacer(modifier = Modifier.height(AppTheme.spacings.sm))
                }
            }
        }
    }
}

@Composable
fun WorkoutRow(exercise: Exercise, expanded: Boolean) {
    ExpandableContent(additionalContent = {
        Spacer(modifier = Modifier.height(AppTheme.spacings.xs))
        Text(
            text = exercise.comment,
            style = AppTheme.typography.body1,
        )
    },
        expanded = expanded) {
        Text(text = exercise.name, style = AppTheme.typography.h2)
    }
}

@Preview(name = "Light Mode", widthDp = 1024)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 320
)
@Composable
fun PreviewWorkoutOverviewCard() {
    AppTheme {
        androidx.compose.material.Surface {
            WorkoutOverviewCard(title = "Your Workout",
                workout = Workout(exercises = listOf(Exercise(name = "Rows", "Rows Cmt"),
                    Exercise(name = "Front Press", "Press Cmt"),
                    Exercise(name = "Deadlift", "DL Cmt"), Exercise(name = "Squats", "Squats Cmt")),
                    "Wkt Cmt"))
        }
    }
}