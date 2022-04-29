@file:OptIn(ExperimentalPermissionsApi::class)

package com.readysetmove.personalworkouts.android.permissions

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme


object GrantPermissionsScreen {
    const val ROUTE = "grant-permissions"
}

@Composable
fun GrantPermissionsScreen(btPermissions: MultiplePermissionsState) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.grant_permissions__screen_title)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                Modifier.semantics { contentDescription = title }
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .verticalScroll(scrollState)
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)
        ) {
            Text("Please grant BTLE permissions")
            Button(onClick = { btPermissions.launchMultiplePermissionRequest() }) {
                Text("Grant BTLE permissions")
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
fun PreviewDeviceManagementOverviewScreen() {
    val btPermissionsGranted = rememberMultiplePermissionsState(
        listOf()
    )
    AppTheme {
        GrantPermissionsScreen(btPermissions = btPermissionsGranted)
    }
}