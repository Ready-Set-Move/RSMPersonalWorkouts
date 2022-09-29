package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.IsTimestampProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.orderBy
import io.github.aakira.napier.Napier

interface IsWorkoutRepository {
    suspend fun fetchLatestWorkoutForUser(userId: String): Workout
    suspend fun saveWorkout(userId: String, workout: Workout)
}

class WorkoutRepository(val timestampProvider: IsTimestampProvider): IsWorkoutRepository {
    private val db = Firebase.firestore

    init {
//        db.useEmulator("10.0.2.2", 8080)
//        db.setSettings(persistenceEnabled = false)
    }

    override suspend fun saveWorkout(userId: String, workout: Workout) {
        val id = timestampProvider.getCurrentDate()
        db.collection("users").document(userId)
            .collection("workouts").document(id).set(workout.copy(id = id))
    }

    override suspend fun fetchLatestWorkoutForUser(userId: String): Workout {
        db.collection("users").document(userId)
            .collection("workouts")
            .orderBy("id", direction = Direction.DESCENDING)
            .limit(1)
            .get().documents.let {
                if (it.isEmpty()) {
                    Napier.d("No document found for user $userId. Returning test workout.")
                    return WorkoutBuilder.workout {
                        exercise("Front Press", position = "19") {
                            set(Set(30, duration = 3, restTime = 3), repeat = 2)
                        }
                        exercise("Ex 2", position = "Some position description") {
                            set(Set(100, duration = 3, restTime = 3), repeat = 1)
                        }
                    }
                }
                val document = it[0]
                Napier.d("Document found for user $userId: ${document.id} | ${document.metadata}")
                return document.data()
            }
    }
}