package com.readysetmove.personalworkouts.workout.results

import kotlinx.coroutines.delay

interface IsWorkoutResultsRepository {
    suspend fun storeResults(workoutResults: WorkoutResults)

    suspend fun rateExercise(
        comment: String,
        rating: Int,
        exercise: String,
    )
}

class WorkoutResultsRepository: IsWorkoutResultsRepository {
    override suspend fun storeResults(workoutResults: WorkoutResults) {
        delay(10)
        TODO("store results")
    }

    override suspend fun rateExercise(
        comment: String,
        rating: Int,
        exercise: String,
    ) {
        delay(10)
        TODO("store exercise rating")
    }
}