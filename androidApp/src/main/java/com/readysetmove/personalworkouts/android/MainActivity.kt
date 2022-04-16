package com.readysetmove.personalworkouts.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.readysetmove.personalworkouts.android.navigation.Screen
import com.readysetmove.personalworkouts.android.settings.SettingsScreen
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.android.workout.WorkoutScreen
import com.readysetmove.personalworkouts.android.workout.overview.overview.WorkoutOverviewScreen

data class Exercise(val name: String, val comment: String)

data class Workout(val exercises: List<Exercise>, val comment: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            AppTheme {
                Scaffold(topBar = {
                    TopAppBar(title = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        Text(text = currentDestination?.route?.let {
                            stringResource(id = Screen.valueOf(it).resourceId)
                        } ?: "")
                    })
                }, floatingActionButton = {
                    ExtendedFloatingActionButton(text = { Text(text = "FAB") },
                        onClick = { navController.navigate(Screen.Workout.name) })
                }) { innerPadding ->
                    NavHost(navController = navController,
                        startDestination = Screen.WorkoutOverview.name,
                        modifier = Modifier.padding(innerPadding)
                    )
                    {
                        composable(route = Screen.WorkoutOverview.name) {
                            WorkoutOverviewScreen(Workout(exercises = listOf(Exercise(name = "Rows",
                                comment = "Rows Cmt"),
                                Exercise(name = "Front Press", comment = "Press Cmt"),
                                Exercise(name = "Deadlift", comment = "DL Cmt")),
                                comment = "Wkt Cmt"))
                        }
                        composable(route = Screen.Settings.name) {
                            SettingsScreen()
                        }
                        composable(route = Screen.Workout.name) {
                            WorkoutScreen()
                        }
                    }
                }
            }
        }
    }
}
