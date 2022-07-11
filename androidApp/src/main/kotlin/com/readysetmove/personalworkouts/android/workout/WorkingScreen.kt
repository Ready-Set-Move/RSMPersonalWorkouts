package com.readysetmove.personalworkouts.android.workout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun WorkingScreen(
    tractionGoal: Int,
    timeToWork: Long,
    currentLoad: Float,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            Text(text = "${(currentLoad - tractionGoal).roundToInt()}", style = AppTheme.typography.h1)
            Text(text = "${if (timeToWork > 1000) ((timeToWork-1)/1000)+1 else "%.1f".format((timeToWork).toFloat()/1000)}s", style = AppTheme.typography.h2)
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
fun WorkingScreenPreview() {
    AppTheme {
        WorkingScreen(
            tractionGoal = 50,
            timeToWork = 5100,
            currentLoad = 10f,
        )
    }
}