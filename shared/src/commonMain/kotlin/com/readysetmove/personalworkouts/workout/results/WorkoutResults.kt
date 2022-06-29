package com.readysetmove.personalworkouts.workout.results

import com.readysetmove.personalworkouts.device.Traction

data class SetResult(val tractionGoal: Long, val tractions: List<Traction>)
data class WorkoutResults(val workoutId: String, val exercises: Map<String, Map<Int, SetResult>>)

fun WorkoutResults?.copyWithAddedResults(setWithResult: Pair<Int, SetResult>, exerciseName: String, workoutId: String): WorkoutResults {
    // TODO: unit tests
    if (this == null) {
        // no results yet? create everything from scratch for the first result
        return WorkoutResults(
            workoutId = workoutId,
            exercises = mapOf(exerciseName to mapOf(setWithResult))
        )
    }

    val newExerciseResults =
        exercises[exerciseName]?.toMutableMap()?.plus(setWithResult)
            ?: mapOf(setWithResult) // no results for this exercise yet? create new results map

    return this.copy(exercises = exercises.plus(exerciseName to newExerciseResults))
}