package com.readysetmove.personalworkouts.android.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.theme.AppTheme

@Composable
fun SettingsScreen() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .verticalScroll(scrollState)
        .padding(AppTheme.spacings.md)) {
        Text(text = "Some Setting")
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
fun PreviewSettingsScreen() {
    AppTheme {
        SettingsScreen()
    }
}