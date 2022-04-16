package com.readysetmove.personalworkouts.android.navigation

import androidx.annotation.StringRes
import com.readysetmove.personalworkouts.android.R

enum class Screen(@StringRes val resourceId: Int) {
    WorkoutOverview(R.string.workout_overview__screen_title),
    Workout(R.string.workout__screen_title),
    Settings(R.string.settings__screen_title)
}