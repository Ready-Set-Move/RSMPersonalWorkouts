package com.readysetmove.personalworkouts.android

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.readysetmove.personalworkouts.android.device.Device
import com.readysetmove.personalworkouts.android.device.management.DeviceManagementOverviewScreen
import com.readysetmove.personalworkouts.android.permissions.GrantPermissionsScreen
import com.readysetmove.personalworkouts.android.settings.SettingsScreen
import com.readysetmove.personalworkouts.android.workout.WorkoutScreen
import com.readysetmove.personalworkouts.android.workout.overview.WorkoutOverviewScreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RSMNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter by remember {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mutableStateOf(bluetoothManager.adapter)
    }
    var btEnabled by rememberSaveable {
        mutableStateOf(bluetoothAdapter.isEnabled)
    }
    DisposableEffect(context) {
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                btEnabled = bluetoothAdapter.isEnabled
            }
        }
        context.registerReceiver(broadcast, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }

    var scanningInProgress by rememberSaveable {
        mutableStateOf(false)
    }
    var devices by rememberSaveable {
        mutableStateOf(listOf(Device(name = "Test 1"), Device(name = "Test 2")))
    }
    DisposableEffect(context) {
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                btEnabled = bluetoothAdapter.isEnabled
            }
        }
        context.registerReceiver(broadcast, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }
    if (scanningInProgress) {
        LaunchedEffect(true) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }
                bluetoothAdapter.startDiscovery()
            }
            delay(1000)
            devices = listOf(Device(name = "Scanned 1"), Device(name = "Scanned 2"))
        }
    }

    val btPermissions = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT >= 31) listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) else listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    if (!btPermissions.allPermissionsGranted) {
        GrantPermissionsScreen(btPermissions = btPermissions)
        return
    }

    val requestActivateBTLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        btEnabled = result.resultCode == Activity.RESULT_OK
    }

    if (!btEnabled) {
        LaunchedEffect(btEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestActivateBTLauncher.launch(enableBtIntent)
        }
        Text("BT not enabled")
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
                onStartWorkout = { navController.navigate(WorkoutScreen.ROUTE) }
            )
        }
        composable(route = SettingsScreen.ROUTE) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = WorkoutScreen.ROUTE) {
            WorkoutScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(route = DeviceManagementOverviewScreen.ROUTE) {
            DeviceManagementOverviewScreen(
                devices = devices,
                scanningInProgress = scanningInProgress,
                onStartScan = {
                    scanningInProgress = true
                },
                onStopScan = {
                    scanningInProgress = false
                }
            ) {
                // TODO: select device action
            }
        }
    }
}
