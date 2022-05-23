package com.readysetmove.personalworkouts.workout

object EntityMocks {
    val SET = Set(tractionGoal = 50f, duration = 6f, restTime = 5f)
    private val SHORT_SET = Set(tractionGoal = 10f, duration = 2f, restTime = 1f)
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
                sets = listOf(Set(tractionGoal = 1337f, duration = .1f, restTime = .2f)))),
            comment = "Only one set")
}
