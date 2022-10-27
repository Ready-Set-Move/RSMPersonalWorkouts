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
import com.readysetmove.personalworkouts.android.ProfileProvider
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.app.AppAction
import com.readysetmove.personalworkouts.app.AppSideEffect
import com.readysetmove.personalworkouts.app.AppStore
import com.readysetmove.personalworkouts.device.ConnectionState
import com.readysetmove.personalworkouts.device.DeviceAction
import com.readysetmove.personalworkouts.device.IsDeviceStore
import com.readysetmove.personalworkouts.workout.WorkoutBuilder
import kotlinx.coroutines.flow.filterIsInstance
import org.koin.androidx.compose.get


object DeviceManagementOverviewScreen {
    const val ROUTE = "device-management-overview"
}

@Composable
fun DeviceManagementOverviewScreen(deviceStore: IsDeviceStore = get(), appStore: AppStore = get(), onNavigateBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.device_management_overview__screen_title)
    val deviceState = deviceStore.observeState().collectAsState()
    DisposableEffect(deviceState.value.connectionConfiguration) {
        if (deviceState.value.connectionConfiguration != null) {
            deviceStore.dispatch(DeviceAction.ScanAndConnect)
        }
        onDispose {
            deviceStore.dispatch(DeviceAction.ResetConnection)
        }
    }
    val workoutSaved = appStore.observeSideEffect().filterIsInstance<AppSideEffect.WorkoutSaved>().collectAsState(initial = null)
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
            DeviceOverviewCard(
                deviceName = deviceState.value.deviceConfiguration?.name ?: "Not connected",
                currentWeight = deviceState.value.traction,
                deviceConfiguration = deviceState.value.deviceConfiguration,
                onReadSettings = { deviceStore.dispatch(DeviceAction.ReadSettings) },
                onCalibrate = { deviceStore.dispatch(DeviceAction.Calibrate) },
                onSetTara = { deviceStore.dispatch(DeviceAction.SetTara) },
            )
            if (deviceState.value.connectionState == ConnectionState.CONNECTING) {
                Spacer(modifier = Modifier.height(AppTheme.spacings.md))
                CircularProgressIndicator()
            } else if (deviceState.value.deviceConfiguration == null) {
                Button(onClick = { deviceStore.dispatch(DeviceAction.ScanAndConnect) }) {
                    Text(text = "Connect")
                }
            }
            if (ProfileProvider.isDevMode) {
                Button(onClick = {
                    appStore.dispatch(AppAction.SaveWorkout(WorkoutBuilder.jonas()))
                }) {
                    Text(text = "Jonas")
                }
                Button(onClick = {
                    appStore.dispatch(AppAction.SaveWorkout(WorkoutBuilder.flo()))
                }) {
                    Text(text = "Flo")
                }
                Button(onClick = {
                    appStore.dispatch(AppAction.SaveWorkout(WorkoutBuilder.priya()))
                }) {
                    Text(text = "Priya")
                }
                Button(onClick = {
                    appStore.dispatch(AppAction.SaveWorkout(WorkoutBuilder.peter()))
                }) {
                    Text(text = "Peter")
                }
                workoutSaved.value?.let {
                    Text(text = it.workout.toString())
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
        DeviceManagementOverviewScreen {}
    }
}