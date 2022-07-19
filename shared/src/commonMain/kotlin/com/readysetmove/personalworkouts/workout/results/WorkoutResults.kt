package com.readysetmove.personalworkouts.workout.results

import com.readysetmove.personalworkouts.device.Traction

data class SetResult(val tractionGoal: Long, val durationGoal: Long, val tractions: List<Traction>, val rating: Int)
data class ExerciseResult(val setResults: Map<Int, SetResult>, val rating: Int? = null)
data class WorkoutResults(val workoutId: String, val exercises: Map<String, ExerciseResult>)

fun WorkoutResults?.copyWithSetResults(setWithResult: Pair<Int, SetResult>, exerciseName: String, workoutId: String): WorkoutResults {
    // TODO: unit tests
    if (this == null) {
        // no results yet? create everything from scratch for the first result
        return WorkoutResults(
            workoutId = workoutId,
            exercises = mapOf(exerciseName to ExerciseResult(
                setResults = mapOf(setWithResult),
            ))
        )
    }

    val newSetResults =
        exercises[exerciseName]?.setResults?.toMutableMap()?.plus(setWithResult)
            ?: mapOf(setWithResult) // no results for this exercise yet? create new results map

    return this.copy(exercises = exercises.plus(
        exerciseName to ExerciseResult(setResults =  newSetResults)
    ))
}