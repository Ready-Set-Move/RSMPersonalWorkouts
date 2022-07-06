package com.readysetmove.personalworkouts.workout

import kotlinx.serialization.Serializable

@Serializable
data class Set(val tractionGoal: Int, val duration: Int = 6, val restTime: Int = 30)