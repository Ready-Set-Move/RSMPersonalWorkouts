package com.readysetmove.personalworkouts.android

import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat.getInsetsController
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.readysetmove.personalworkouts.android.device.management.DeviceManagementOverviewScreen
import com.readysetmove.personalworkouts.android.settings.SettingsScreen
import com.readysetmove.personalworkouts.android.user.LoginScreen
import com.readysetmove.personalworkouts.android.workout.WorkoutScreen
import com.readysetmove.personalworkouts.android.workout.overview.WorkoutOverviewScreen
import com.readysetmove.personalworkouts.app.AppStore
import com.readysetmove.personalworkouts.device.DeviceSideEffect
import com.readysetmove.personalworkouts.device.IsDeviceStore
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressSideEffect
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressStore
import kotlinx.coroutines.flow.filterIsInstance
import org.koin.androidx.compose.inject

@Composable
fun RSMNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val appStore: AppStore by inject()
    val appState = appStore.observeState().collectAsState()
    val workoutProgressStore: WorkoutProgressStore by inject()
    val workoutSideEffects = workoutProgressStore.observeSideEffect().collectAsState(null)
    val deviceStore: IsDeviceStore by inject()

    val disConnectedSideEffect =
        deviceStore.observeSideEffect().filterIsInstance<DeviceSideEffect.DeviceDisconnected>()
            .collectAsState(null)
    LaunchedEffect(disConnectedSideEffect.value) {
        if (disConnectedSideEffect.value != null) {
            Toast.makeText(context, "Device Disconnected", Toast.LENGTH_LONG).show()
        }
    }

    val view = LocalView.current
    LaunchedEffect(workoutSideEffects.value) {
        when(workoutSideEffects.value) {
            is WorkoutProgressSideEffect.NewWorkoutProgressStarted -> {
                navController.navigate(WorkoutScreen.ROUTE)
                if (!view.isInEditMode) {
                    val currentWindow = (view.context as? Activity)?.window
                        ?: throw Exception("Not in an activity - unable to get Window reference")
                    getInsetsController(currentWindow, view)?.let {
                        it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        it.hide(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
            is WorkoutProgressSideEffect.WorkoutFinished -> {
                if (!view.isInEditMode) {
                    val currentWindow = (view.context as? Activity)?.window
                        ?: throw Exception("Not in an activity - unable to get Window reference")
                    getInsetsController(currentWindow, view)?.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

//  TODO: move to own screen and react to current settings later
//    val bluetoothAdapter: BluetoothAdapter by remember {
//        val bluetoothManager =
//            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        mutableStateOf(bluetoothManager.adapter)
//    }
//    DisposableEffect(context) {
//        val broadcast = object : BroadcastReceiver() {
//            override fun onReceive(context: Context?, intent: Intent?) {
//                // TODO: move logic to device store?
//                if (bluetoothAdapter.isEnabled &&
//                    deviceState.value.connectionConfiguration is ConnectionConfiguration.BLEConnection
//                )
//                    deviceStore.dispatch(DeviceAction.ScanAndConnect)
//            }
//        }
//        context.registerReceiver(broadcast, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
//        onDispose {
//            context.unregisterReceiver(broadcast)
//        }
//    }
//    val btPermissions =
//        rememberMultiplePermissionsState(AndroidBluetoothService.REQUIRED_PERMISSIONS)
//    LaunchedEffect(btPermissions.allPermissionsGranted) {
//        btStore.dispatch(BluetoothAction.SetBluetoothPermissionsGranted(btPermissions.allPermissionsGranted))
//    }
//    val requestActivateBTLauncher =
//        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                deviceStore.dispatch(DeviceAction.ScanAndConnect)
//            }
//        }
//    if (!btState.value.bluetoothEnabled) {
//        LaunchedEffect(btState.value.bluetoothEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            requestActivateBTLauncher.launch(enableBtIntent)
//        }
//    }
//    if (!btState.value.bluetoothPermissionsGranted && !ProfileProvider.isDevMode) {
//        GrantPermissionsScreen(btPermissions = btPermissions)
//        return
//    }

    val startDestination = when {
        (appState.value.user == null) -> LoginScreen.ROUTE
//        ProfileProvider.isDevMode -> WorkoutOverviewScreen.ROUTE
        else -> DeviceManagementOverviewScreen.ROUTE
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    )
    {
        composable(route = LoginScreen.ROUTE) {
            LoginScreen()
        }
// TODO: react to permissions in device management screen and to error side effects
//        composable(route = GrantPermissionsScreen.ROUTE) {
//            GrantPermissionsScreen(btPermissions = btPermissions)
//        }
        composable(route = WorkoutOverviewScreen.ROUTE) {
            WorkoutOverviewScreen(
                userName = appState.value.user?.displayName ?: "Not logged in",
                workout = appState.value.workout,
                onNavigateBack = { navController.navigate(LoginScreen.ROUTE) }
            )
        }
        composable(route = SettingsScreen.ROUTE) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = WorkoutScreen.ROUTE) {
            // TODO: extract workout parameters from route to deeplink to exercise & set
            WorkoutScreen {
                navController.popBackStack()
            }
        }
        composable(route = DeviceManagementOverviewScreen.ROUTE) {
            DeviceManagementOverviewScreen(onNavigateBack = {
                navController.navigate(WorkoutOverviewScreen.ROUTE)
            })
        }
    }
}
