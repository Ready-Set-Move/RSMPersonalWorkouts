package com.readysetmove.personalworkouts.android.workout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.readysetmove.personalworkouts.android.theme.AppTheme

@Composable
fun RestingIndicator(
    timeToRest: Long,
    duration: Int,
) {
    Box(
        modifier = Modifier
            .size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        val timeProgress = timeToRest/(duration*1000f)
        CircularProgressIndicator(
            progress = timeProgress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 15.dp,
        )
        Text(
            text = "${if (timeToRest > 1000) ((timeToRest-1)/1000)+1 else "%.1f".format((timeToRest).toFloat()/1000)}s",
            style = AppTheme.typography.button,
            modifier = Modifier.padding(all = 0.dp),
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 1024
)
@Composable
fun PreviewRestingScreen() {
    AppTheme {
        RestingIndicator(
            timeToRest = 24000,
            duration = 30,
        )
    }
}