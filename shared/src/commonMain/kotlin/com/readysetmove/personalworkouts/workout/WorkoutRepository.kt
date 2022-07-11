package com.readysetmove.personalworkouts.workout

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import io.github.aakira.napier.Napier

interface IsWorkoutRepository {
    suspend fun fetchLatestWorkoutForUser(userId: String): Workout
}

class WorkoutRepository: IsWorkoutRepository {
    private val db = Firebase.firestore

    init {
        db.useEmulator("10.0.2.2", 8080)
        db.setSettings(persistenceEnabled = false)
    }

    override suspend fun fetchLatestWorkoutForUser(userId: String): Workout {
        return when(userId) {
            // Levi
            "lZn5Z8lt6lVdIiFsoiHSqRbNi3u1" -> WorkoutBuilder.workout {
                exercise("Bench Press", position = "18") {
                    warmup(xMin = 10, min = 15, med = 20, max = 30)
                    set(Set(30, duration = 6), repeat = 4)
                }
                exercise("Squat", position = "9") {
                    warmup(min = 22, med = 35, max = 45)
                    set(Set(45, duration = 12), repeat = 4)
                }
                exercise("Shrugs", position = "6") {
                    warmup(min = 25, med = 35, max = 55)
                    set(Set(55, duration = 9), repeat = 4)
                }
            }
            // Flo
            "grT5yFPAYtREAP71zVFS3KTiST62" -> WorkoutBuilder.workout {
                exercise("Deadlift", position = "0") {
                    warmup(xMin = 30, min = 60, med = 90, max = 120)
                    set(Set(120, duration = 15), repeat = 3)
                    set(Set(120, duration = 12), repeat = 1)
                }
                exercise("Drag Curls", position = "5") {
                    warmup(xMin = 10, min = 15, med = 20, max = 35)
                    set(Set(35, duration = 12), repeat = 3)
                }
            }
            "Jose" -> WorkoutBuilder.workout {
                exercise("Front Press", position = "17") {
                    warmup(28,42,59)
                    set(Set(59), repeat = 4)
                }
                exercise("Squat", position = "6") {
                    warmup(55,85,115)
                    set(Set(115), repeat = 5)
                }
                exercise("Shrugs", position = "6") {
                    set(Set(0, duration = 15, restTime = 10))
                    set(Set(0, duration = 10, restTime = 10))
                    set(Set(0, duration = 5, restTime = 15))
                    set(Set(0, duration = 1))
                    set(Set(0), repeat = 3)
                }
            }
            "Alex" -> WorkoutBuilder.workout {
                exercise("Front Press", position = "19") {
                    warmup(xMin = 8, min = 16, med = 24, max = 30)
                    set(Set(30, duration = 12), repeat = 4)
                }
                exercise("Squat", position = "5") {
                    assessmentTest(min = 30, med = 45, max = 60)
                }
                exercise("Shrugs", position = "6") {
                    assessmentTest(min = 25, med = 35, max = 55)
                }
            }
            // Rob 5.7
            "Rob" ->

                WorkoutBuilder.workout {
                exercise("Shrugs", position = "0") {
                    warmup(min = 50, med = 75, max = 100)
                    set(Set(100, duration = 12), repeat = 4)
                }
//                exercise("Calf Lifts", position = 0f) {
//                    warmup(min = 30, med = 45, max = 65)
//                    set(Set(65, duration = 15), repeat = 3)
//                    set(Set(65, duration = 12), repeat = 1)
//                }
                exercise("Abductor Extensions", position = "0") {
                    assessmentTest(5, 10, 15)
                }
                exercise("Drag Curls", position = "16") {
                    warmup(xMin = 10, min = 18, med = 25, max = 37)
                    set(Set(37, duration = 9), repeat = 4)
                }
//                exercise("Front Press", position = 16f) {
//                    warmup(min = 25, med = 40, max = 55)
//                    set(Set(55), repeat = 4)
//                }
                exercise("Rotator Cuff Extensions", position = "-1") {
                    warmup(xMin = 5, min = 5, med = 8, max = 11)
                    set(Set(11), repeat = 5)
                }
            }
            // Rob google user
//            "npt9ZyOesMaYEIaZjGJly8w7C773" -> {
            "npt9ZyOesMaYEIaZjGJly8w7C773a" -> {
                val document = db.collection("users").document("npt9ZyOesMaYEIaZjGJly8w7C773")
                    .collection("workouts")
//                    .orderBy(FieldPath("users/workouts").documentId, direction = Direction.DESCENDING)
                    .limit(1)
                    .get().documents[0]
                Napier.d("Document found for user $userId: ${document.id} | ${document.metadata}")
                document.data()
            }
            else -> return WorkoutBuilder.workout {
                exercise("Front Press", position = "19") {
                    set(Set(30, duration = 3, restTime = 3), repeat = 2)
                }
                exercise("Ex 2", position = "Some position description") {
                    set(Set(100, duration = 3, restTime = 3), repeat = 1)
                }
            }
        }
    }
}