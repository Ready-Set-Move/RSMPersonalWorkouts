package com.readysetmove.personalworkouts.android

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.readysetmove.personalworkouts.android.device.management.DeviceManagementOverviewScreen
import com.readysetmove.personalworkouts.android.permissions.GrantPermissionsScreen
import com.readysetmove.personalworkouts.android.settings.SettingsScreen
import com.readysetmove.personalworkouts.android.workout.WorkoutScreen
import com.readysetmove.personalworkouts.android.workout.overview.WorkoutOverviewScreen
import com.readysetmove.personalworkouts.app.AppAction
import com.readysetmove.personalworkouts.app.AppStore
import com.readysetmove.personalworkouts.app.lastSetResult
import com.readysetmove.personalworkouts.bluetooth.AndroidBluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothAction
import com.readysetmove.personalworkouts.bluetooth.BluetoothSideEffect
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import com.readysetmove.personalworkouts.workout.WorkoutSideEffect
import com.readysetmove.personalworkouts.workout.WorkoutStore
import com.readysetmove.personalworkouts.workout.activeExercise
import com.readysetmove.personalworkouts.workout.activeSet
import kotlinx.coroutines.flow.filterIsInstance
import org.koin.androidx.compose.inject

@Composable
fun RSMNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val appStore: AppStore by inject()
    val appState = appStore.observeState().collectAsState()
    val workoutStore: WorkoutStore by inject()
    val workoutState = workoutStore.observeState().collectAsState()
    val workoutSideEffects = workoutStore.observeSideEffect().collectAsState(null)
    val btStore: BluetoothStore by inject()
    val btState = btStore.observeState().collectAsState()

    val bluetoothAdapter: BluetoothAdapter by remember {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mutableStateOf(bluetoothManager.adapter)
    }
    DisposableEffect(context) {
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                btStore.dispatch(BluetoothAction.SetBluetoothEnabled(bluetoothAdapter.isEnabled))
            }
        }
        context.registerReceiver(broadcast, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }

    val btPermissions =
        rememberMultiplePermissionsState(AndroidBluetoothService.REQUIRED_PERMISSIONS)

    LaunchedEffect(btPermissions.allPermissionsGranted) {
        btStore.dispatch(BluetoothAction.SetBluetoothPermissionsGranted(btPermissions.allPermissionsGranted))
    }

    val requestActivateBTLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            btStore.dispatch(BluetoothAction.SetBluetoothEnabled(result.resultCode == Activity.RESULT_OK))
        }

    if (!btState.value.bluetoothEnabled) {
        LaunchedEffect(btState.value.bluetoothEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestActivateBTLauncher.launch(enableBtIntent)
        }
    }
    val disConnectedSideEffect =
        btStore.observeSideEffect().filterIsInstance<BluetoothSideEffect.DeviceDisConnected>()
            .collectAsState(null)
    LaunchedEffect(disConnectedSideEffect.value) {
        if (disConnectedSideEffect.value != null) {
            Toast.makeText(context, "Device Disconnected", Toast.LENGTH_LONG).show()
        }
    }

    if (!btState.value.bluetoothPermissionsGranted) {
        GrantPermissionsScreen(btPermissions = btPermissions)
        return
    }

    LaunchedEffect(workoutSideEffects.value) {
        if (workoutSideEffects.value is WorkoutSideEffect.NewWorkoutStarted) {
            navController.navigate(WorkoutScreen.ROUTE)
        }
    }

    NavHost(
        navController = navController,
        startDestination = DeviceManagementOverviewScreen.ROUTE,
//        startDestination = WorkoutOverviewScreen.ROUTE,
    )
    {
        composable(route = GrantPermissionsScreen.ROUTE) {
            GrantPermissionsScreen(btPermissions = btPermissions)
        }
        composable(route = WorkoutOverviewScreen.ROUTE) {
            WorkoutOverviewScreen(
                appState.value.workout,
                onStartWorkout = { appStore.dispatch(AppAction.StartWorkout) }
            )
        }
        composable(route = SettingsScreen.ROUTE) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = WorkoutScreen.ROUTE) {
            // TODO: extract workout parameters from route to deeplink to exercise & set

            val workoutProgress = workoutState.value.workoutProgress
                ?: return@composable Text(text = "No Workout set")

            WorkoutScreen(
                exercise = workoutProgress.activeExercise(),
                set = workoutProgress.activeSet(),
                timeToWork = workoutState.value.timeToWork,
                timeToRest = workoutState.value.timeToRest,
                onStartSet = { appStore.dispatch(AppAction.StartNextSet) },
                onNavigateBack = { navController.popBackStack() },
                setResults = appState.value.workoutResults?.lastSetResult()?.tractions,
            )
        }
        composable(route = DeviceManagementOverviewScreen.ROUTE) {
            DeviceManagementOverviewScreen(onNavigateBack = {
                navController.navigate(WorkoutOverviewScreen.ROUTE)
            })
        }
    }
}
