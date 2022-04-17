package com.readysetmove.personalworkouts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.readysetmove.personalworkouts.android.RSMNavHost
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    lateinit var navHostController: NavHostController

    @Before
    fun setup() {
        composeTestRule.setContent {
            navHostController = rememberNavController()
            RSMNavHost(navController = navHostController)
        }
    }

    @Test
    fun appStartsAtWorkoutOverview() {
        composeTestRule
            .onNodeWithContentDescription("Workout Overview")
            .assertIsDisplayed()
    }

    @Test
    fun startWorkoutLeadsFromOverview2Workout() {
        composeTestRule
            .onNodeWithContentDescription("Start Workout")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Active Workout")
            .assertIsDisplayed()
    }

    @Test
    fun backButtonLeadsOneStepBack() {
        composeTestRule
            .onNodeWithContentDescription("Start Workout")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Go back")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Workout Overview")
            .assertIsDisplayed()
    }
}