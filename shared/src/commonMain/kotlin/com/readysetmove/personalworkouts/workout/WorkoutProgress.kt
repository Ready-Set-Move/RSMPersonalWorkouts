package com.readysetmove.personalworkouts.workout

data class WorkoutProgress(
    val workout: Workout,
    val activeExerciseIndex: Int = 0,
    val activeSetIndex: Int = 0,
)

fun WorkoutProgress.activeExercise(): Exercise {
    return workout.exercises[activeExerciseIndex]
}

fun WorkoutProgress.activeSet(): Set {
    return activeExercise().sets[activeSetIndex]
}
