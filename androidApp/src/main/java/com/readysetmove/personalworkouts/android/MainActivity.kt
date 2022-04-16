package com.readysetmove.personalworkouts.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                NavHost(
                    navController = navController,
                    startDestination = WorkoutOverviewScreen.ROUTE,
                )
                {
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
                }
            }
        }
    }
}
