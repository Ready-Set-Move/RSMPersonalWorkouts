package com.readysetmove.personalworkouts.workout

data class Workout(val id: String, val exercises: List<Exercise>, val comment: String)

fun Workout.isNotValid(errorCallback: ((exception: WorkoutExceptions) -> Unit)?): Boolean {
    if (exercises.isEmpty()) {
        errorCallback?.invoke(WorkoutExceptions.EmptyWorkoutException(this))
        return true
    }
    exercises.forEach { exercise ->
        if (exercise.sets.isEmpty()) {
            errorCallback?.invoke(WorkoutExceptions.EmptyExerciseException(
                workout = this,
                exercise = exercise,
            ))
            return true
        }
    }
    return false
}
