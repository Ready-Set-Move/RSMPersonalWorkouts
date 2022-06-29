package com.readysetmove.personalworkouts.workout.results

import kotlinx.coroutines.delay

interface IsWorkoutResultsRepository {
    suspend fun storeResults(workoutResults: WorkoutResults)
}

class WorkoutResultsRepository: IsWorkoutResultsRepository {
    override suspend fun storeResults(workoutResults: WorkoutResults) {
        // TODO: store results if possible
        delay(10)
    }
}