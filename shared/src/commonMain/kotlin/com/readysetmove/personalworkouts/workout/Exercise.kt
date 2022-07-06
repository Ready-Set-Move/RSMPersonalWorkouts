package com.readysetmove.personalworkouts.workout

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(val name: String, val comment: String, val sets: List<Set>, val position: String)