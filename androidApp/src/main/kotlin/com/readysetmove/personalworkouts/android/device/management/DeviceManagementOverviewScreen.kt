package com.readysetmove.personalworkouts.android.device.management

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.preview.PreviewBluetoothService
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.bluetooth.BluetoothAction
import com.readysetmove.personalworkouts.bluetooth.BluetoothState
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import com.readysetmove.personalworkouts.device.IsDeviceStore
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.get


object DeviceManagementOverviewScreen {
    const val ROUTE = "device-management-overview"
}

@Composable
fun DeviceManagementOverviewScreen(store: BluetoothStore = get(), deviceStore: IsDeviceStore = get(), onNavigateBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.device_management_overview__screen_title)
    val state = store.observeState().collectAsState()
    DisposableEffect(state.value.bluetoothEnabled) {
        if (state.value.bluetoothEnabled) {
            store.dispatch(BluetoothAction.ScanAndConnect)
        }
        onDispose { store.dispatch(BluetoothAction.StopScanning) }
    }
    val deviceState = deviceStore.observeState().collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigation__back))
                    }
                },
                modifier = Modifier.semantics { contentDescription = title }
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .verticalScroll(scrollState)
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)
        ) {
            state.value.activeDevice?.let {
                DeviceOverviewCard(
                    deviceName = it,
                    currentWeight = deviceState.value.traction,
                    deviceConfiguration = deviceState.value.deviceConfiguration,
                    onReadSettings = { store.dispatch(BluetoothAction.ReadSettings) },
                    onCalibrate = { store.dispatch(BluetoothAction.Calibrate) },
                    onSetTara = { store.dispatch(BluetoothAction.SetTara) },
                )
            }
            if (state.value.scanning) {
                Spacer(modifier = Modifier.height(AppTheme.spacings.md))
                CircularProgressIndicator()
            } else if (state.value.activeDevice == null) {
                Button(onClick = { store.dispatch(BluetoothAction.ScanAndConnect) }) {
                    Text(text = "Connect")
                }
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
            store = BluetoothStore(
                bluetoothService = PreviewBluetoothService,
                initialState = BluetoothState(bluetoothEnabled = true),
                ioDispatcher = Dispatchers.IO,
                mainDispatcher = Dispatchers.Main,
            )
        ) {}
    }
}