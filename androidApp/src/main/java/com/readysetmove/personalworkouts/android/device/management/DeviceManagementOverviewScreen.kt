package com.readysetmove.personalworkouts.android.device.management

import android.Manifest
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.device.Device
import com.readysetmove.personalworkouts.android.theme.AppTheme


object DeviceManagementOverviewScreen {
    const val ROUTE = "device-management-overview"
}

@Composable
fun DeviceManagementOverviewScreen(devices: List<Device>, scanningInProgress: Boolean, onStartScan: () -> Unit, onStopScan: () -> Unit, onDeviceSelected: (device: Device) -> Unit) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.device_management_overview__screen_title)
    val currentOnStartScan by rememberUpdatedState(onStartScan)
    val currentOnStopScan by rememberUpdatedState(onStopScan)
    DisposableEffect(onStartScan, onStopScan) {
        currentOnStartScan()
        onDispose { currentOnStopScan() }
    }
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
            devices.map {
                DeviceOverviewCard(device = it) { device ->
                    onDeviceSelected(device)
                }
                Spacer(modifier = Modifier.height(AppTheme.spacings.sm))
            }
            if (scanningInProgress) {
                Spacer(modifier = Modifier.height(AppTheme.spacings.md))
                CircularProgressIndicator()
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
    AppTheme {
        DeviceManagementOverviewScreen(
            devices = listOf(Device(name = "Dev0"), Device(name = "Dev1")),
            scanningInProgress = true,
            onStartScan = {},
            onStopScan = {},
            onDeviceSelected = {}
        )
    }
}