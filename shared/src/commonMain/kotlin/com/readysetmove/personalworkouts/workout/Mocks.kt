package com.readysetmove.personalworkouts.workout

object Mocks {
    private val set = Set(tractionGoal = 50f, duration = 6f, restTime = 30f)
    private val sets = listOf(set, set, set)
    private val rows = Exercise(name = "Rows", comment = "Rows Cmt", sets = sets)
    private val fp = Exercise(name = "Front Press", comment = "Press Cmt", sets = sets)
    private val dl = Exercise(name = "Deadlift", comment = "DL Cmt", sets = sets)
    private val squats = Exercise(name = "Squats", comment = "Squats Cmt", sets = sets)
    val workout = Workout(exercises = listOf(rows, fp, dl, squats), comment = "Wkt Cmt")
}
