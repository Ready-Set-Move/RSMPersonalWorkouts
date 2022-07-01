package com.readysetmove.personalworkouts.workout

interface IsWorkoutRepository {
    suspend fun fetchLatestWorkoutForUser(userId: String): Workout
}

class WorkoutRepository: IsWorkoutRepository {
    override suspend fun fetchLatestWorkoutForUser(userId: String): Workout {
        return when(userId) {
            // Flo
            "grT5yFPAYtREAP71zVFS3KTiST62" -> WorkoutBuilder.workout {
                exercise("Deadlift", position = 0f) {
                    warmup(xMin = 30, min = 60, med = 90, max = 120)
                    set(Set(120000, duration = 15000), repeat = 2)
                    set(Set(120000, duration = 12000), repeat = 2)
                }
                exercise("Drag Curls", position = 5f) {
                    warmup(xMin = 10, min = 15, med = 20, max = 35)
                    set(Set(35000, duration = 9000), repeat = 4)
                }
            }
            "Jose" -> WorkoutBuilder.workout {
                exercise("Front Press", position = 17f) {
                    warmup(28,42,59)
                    set(Set(59000), repeat = 4)
                }
                exercise("Squat", position = 6f) {
                    warmup(55,85,115)
                    set(Set(115000), repeat = 5)
                }
                exercise("Shrugs", position = 6f) {
                    set(Set(0, duration = 15000, restTime = 10000))
                    set(Set(0, duration = 10000, restTime = 10000))
                    set(Set(0, duration = 5000, restTime = 15000))
                    set(Set(0, duration = 1000))
                    set(Set(0), repeat = 3)
                }
            }
            "Alex" -> WorkoutBuilder.workout {
                exercise("Front Press", position = 19f) {
                    warmup(xMin = 8, min = 16, med = 24, max = 30)
                    set(Set(30000, duration = 12000), repeat = 4)
                }
                exercise("Squat", position = 5f) {
                    assessmentTest(min = 30, med = 45, max = 60)
                }
                exercise("Shrugs", position = 6f) {
                    assessmentTest(min = 25, med = 35, max = 55)
                }
            }
            // Rob up to date
            "akmgotyWSNUxYIfEfqUOA9trEDv1" -> WorkoutBuilder.workout {
                exercise("Shrugs", position = 0f) {
                    warmup(min = 50, med = 75, max = 100)
                    set(Set(100000, duration = 12000), repeat = 3)
                }
                exercise("Calf Lifts", position = 0f) {
                    warmup(min = 30, med = 45, max = 65)
                    set(Set(65000, duration = 15000), repeat = 3)
                    set(Set(65000, duration = 12000), repeat = 1)
                }
                exercise("Drag Curls", position = 16f) {
                    warmup(xMin = 10, min = 18, med = 25, max = 37)
                    set(Set(37000, duration = 6000), repeat = 5)
                }
//                exercise("Front Press", position = 16f) {
//                    warmup(min = 25, med = 40, max = 55)
//                    set(Set(55000), repeat = 4)
//                }
                exercise("Rotator Cuff Extensions", position = -1f) {
                    warmup(xMin = 5, min = 5, med = 8, max = 11)
                    set(Set(11000), repeat = 4)
                }
            }
            else -> return WorkoutBuilder.workout {
                exercise("Front Press", position = 19f) {
                    warmup(xMin = 8, min = 16, med = 24, max = 30)
                    set(Set(30000, duration = 12000), repeat = 4)
                }
                exercise("Squat", position = 5f) {
                    assessmentTest(min = 30, med = 45, max = 60)
                }
                exercise("Shrugs", position = 6f) {
                    assessmentTest(min = 25, med = 35, max = 55)
                }
            }
        }
    }
}