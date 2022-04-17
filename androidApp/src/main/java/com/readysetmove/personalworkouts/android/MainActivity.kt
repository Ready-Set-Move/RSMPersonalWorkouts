package com.readysetmove.personalworkouts.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.readysetmove.personalworkouts.android.theme.AppTheme

data class Exercise(val name: String, val comment: String)

data class Workout(val exercises: List<Exercise>, val comment: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            AppTheme {
                RSMNavHost(navController = navController)
            }
        }
    }
}
