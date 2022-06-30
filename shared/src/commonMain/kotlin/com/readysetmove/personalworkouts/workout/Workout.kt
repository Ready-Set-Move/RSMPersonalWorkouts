package com.readysetmove.personalworkouts.workout

data class Workout(val id: String, val exercises: List<Exercise>, val comment: String)

fun Workout.throwIfNotValid(): Boolean {
    if (exercises.isEmpty()) {
        throw WorkoutExceptions.EmptyWorkoutException(this)
    }
    exercises.forEach { exercise ->
        if (exercise.sets.isEmpty()) {
            throw WorkoutExceptions.EmptyExerciseException(
                workout = this,
                exercise = exercise,
            )
        }
    }
    return false
}
