package com.readysetmove.personalworkouts.android.settings

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
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme

object SettingsScreen {
    const val ROUTE = "settings"
}

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings__screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "BACK")
                    }
                })
        }
    ) {innerPadding ->
        Column(modifier = Modifier
            .verticalScroll(scrollState)
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)) {
            Text(text = "Some Setting")
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
fun PreviewSettingsScreen() {
    AppTheme {
        SettingsScreen(onNavigateBack = {})
    }
}