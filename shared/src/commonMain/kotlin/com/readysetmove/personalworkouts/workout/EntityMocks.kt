package com.readysetmove.personalworkouts.workout

object EntityMocks {
    val SET = Set(tractionGoal = 50000, duration = 6000, restTime = 5000)
    private val SHORT_SET = Set(tractionGoal = 10000, duration = 2000, restTime = 1000)
    private val SETS = listOf(SET, SET, SET)
    private val SHORT_SETS = listOf(SHORT_SET, SHORT_SET, SHORT_SET)
    val ROWS = Exercise(name = "Rows", comment = "Rows Cmt", sets = SETS)
    private val FP = Exercise(name = "Front Press", comment = "Press Cmt", sets = SHORT_SETS)
    private val DL = Exercise(name = "Deadlift", comment = "DL Cmt", sets = SHORT_SETS)
    private val SQUATS = Exercise(name = "Squats", comment = "Squats Cmt", sets = SHORT_SETS)
    val WORKOUT = Workout(exercises = listOf(ROWS, FP, DL, SQUATS), comment = "Wkt Cmt")
    val ONE_SET_WORKOUT =
        Workout(
            exercises = listOf(Exercise(
                name = "ONE SET",
                comment = "One Set",
                sets = listOf(Set(tractionGoal = 1337000, duration = 100, restTime = 200)))),
            comment = "Only one set")
}
