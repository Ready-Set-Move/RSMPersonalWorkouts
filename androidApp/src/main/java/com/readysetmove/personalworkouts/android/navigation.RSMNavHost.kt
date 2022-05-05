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
import com.readysetmove.personalworkouts.bluetooth.AndroidBluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothAction
import com.readysetmove.personalworkouts.bluetooth.BluetoothSideEffect
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import kotlinx.coroutines.flow.filterIsInstance
import org.koin.androidx.compose.inject

@Composable
fun RSMNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val btStore: BluetoothStore by inject()
    val state = btStore.observeState().collectAsState()

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

    if (!state.value.bluetoothEnabled) {
        LaunchedEffect(state.value.bluetoothEnabled) {
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

    if (!state.value.bluetoothPermissionsGranted) {
        GrantPermissionsScreen(btPermissions = btPermissions)
        return
    }

    NavHost(
        navController = navController,
//        startDestination = WorkoutOverviewScreen.ROUTE,
        startDestination = DeviceManagementOverviewScreen.ROUTE,
    )
    {
        composable(route = GrantPermissionsScreen.ROUTE) {
            GrantPermissionsScreen(btPermissions = btPermissions)
        }
        composable(route = WorkoutOverviewScreen.ROUTE) {
            WorkoutOverviewScreen(
                Workout(exercises = listOf(Exercise(name = "Rows",
                    comment = "Rows Cmt"),
                    Exercise(name = "Front Press", comment = "Press Cmt"),
                    Exercise(name = "Deadlift", comment = "DL Cmt")),
                    comment = "Wkt Cmt"),
                onStartWorkout = { navController.navigate(DeviceManagementOverviewScreen.ROUTE) }
//                onStartWorkout = { navController.navigate(WorkoutScreen.ROUTE) }
            )
        }
        composable(route = SettingsScreen.ROUTE) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = WorkoutScreen.ROUTE) {
            WorkoutScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = DeviceManagementOverviewScreen.ROUTE) {
            DeviceManagementOverviewScreen(onNavigateBack = {
                navController.navigate(WorkoutOverviewScreen.ROUTE)
            })
        }
    }
}
