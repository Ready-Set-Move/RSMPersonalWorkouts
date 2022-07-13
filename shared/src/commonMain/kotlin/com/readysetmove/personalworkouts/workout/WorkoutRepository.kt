package com.readysetmove.personalworkouts.workout

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import io.github.aakira.napier.Napier

interface IsWorkoutRepository {
    suspend fun fetchLatestWorkoutForUser(userId: String): Workout
    suspend fun saveWorkout(userId: String, workout: Workout)
}

class WorkoutRepository: IsWorkoutRepository {
    private val db = Firebase.firestore

    init {
//        db.useEmulator("10.0.2.2", 8080)
//        db.setSettings(persistenceEnabled = false)
    }

    override suspend fun saveWorkout(userId: String, workout: Workout) {
        db.collection("users").document(userId)
            .collection("workouts").document("1657638883812").set(workout)
    }

    override suspend fun fetchLatestWorkoutForUser(userId: String): Workout {
        db.collection("users").document(userId)
            .collection("workouts")
//                    .orderBy(FieldPath("users/workouts").documentId, direction = Direction.DESCENDING)
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