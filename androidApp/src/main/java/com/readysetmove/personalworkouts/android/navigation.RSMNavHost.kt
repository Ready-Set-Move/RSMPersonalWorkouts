package com.readysetmove.personalworkouts.android

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.readysetmove.personalworkouts.android.settings.SettingsScreen
import com.readysetmove.personalworkouts.android.workout.WorkoutScreen
import com.readysetmove.personalworkouts.android.workout.overview.overview.WorkoutOverviewScreen

@Composable
fun RSMNavHost(navController: NavHostController) {
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