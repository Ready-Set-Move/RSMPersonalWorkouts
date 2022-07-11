package com.readysetmove.personalworkouts.android.workout

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.theme.AppTheme

@Composable
fun RestingScreen(
    timeToRest: Long,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.primary),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(
                text = "${if (timeToRest > 1000) ((timeToRest-1)/1000)+1 else "%.1f ".format((timeToRest).toFloat()/1000)} s",
                style = AppTheme.typography.h1,
                color = AppTheme.colors.onPrimary,
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
fun PreviewRestingScreen() {
    AppTheme {
        RestingScreen(
            timeToRest = 30000,
        )
    }
}