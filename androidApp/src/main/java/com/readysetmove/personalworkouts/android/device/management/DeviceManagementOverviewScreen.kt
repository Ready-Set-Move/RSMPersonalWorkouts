package com.readysetmove.personalworkouts.android.device.management

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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.bluetooth.BluetoothAction
import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import com.readysetmove.personalworkouts.bluetooth.Device
import org.koin.androidx.compose.get


object DeviceManagementOverviewScreen {
    const val ROUTE = "device-management-overview"
}

@Composable
fun DeviceManagementOverviewScreen(store: BluetoothStore = get()) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.device_management_overview__screen_title)
    DisposableEffect(true) {
        store.dispatch(BluetoothAction.ScanAndConnect)
        onDispose { store.dispatch(BluetoothAction.StopScanning) }
    }
    val state = store.observeState().collectAsState()
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
            state.value.activeDevice?.let {
                DeviceOverviewCard(it)
                Spacer(modifier = Modifier.height(AppTheme.spacings.sm))
            }
            if (state.value.scanning) {
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
        DeviceManagementOverviewScreen(store = BluetoothStore(object : BluetoothService {
            override suspend fun scanForDevice(deviceName: String): Device {
                return Device(deviceName)
            }

            override fun stopScan() {}
        }))
    }
}