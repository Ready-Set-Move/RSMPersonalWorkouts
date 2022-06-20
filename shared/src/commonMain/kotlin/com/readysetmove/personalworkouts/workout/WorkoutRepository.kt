package com.readysetmove.personalworkouts.workout

interface IsWorkoutRepository {
    suspend fun fetchLatestWorkoutForUser(userId: String): Workout
}

class WorkoutRepository: IsWorkoutRepository {
    override suspend fun fetchLatestWorkoutForUser(userId: String): Workout {
        return when(userId) {
            "Flo" -> WorkoutBuilder.workout {
                exercise("Deadlift") {
                    warmup(xMin = 30, min = 45, med = 60, max = 90)
                    set(Set(90000, duration = 12000), repeat = 4)
                }
                exercise("Drag Curls") {
                    warmup(xMin = 7, min = 10, med = 14, max = 19)
                    set(Set(19000, duration = 6000), repeat = 4)
                }
            }
            "Jose" -> WorkoutBuilder.workout {
                exercise("Front Press") {
                    set(Set(28000, duration = 10000, restTime = 10000))
                    set(Set(42000, duration = 5000, restTime = 15000))
                    set(Set(59000, duration = 1000))
                    set(Set(59000), repeat = 4)
                }
                exercise("Squat") {
                    set(Set(55000, duration = 10000, restTime = 10000))
                    set(Set(85000, duration = 5000, restTime = 15000))
                    set(Set(115000, duration = 1000))
                    set(Set(115000), repeat = 5)
                }
                exercise("Shrugs") {
                    set(Set(0, duration = 15000, restTime = 10000))
                    set(Set(0, duration = 10000, restTime = 10000))
                    set(Set(0, duration = 5000, restTime = 15000))
                    set(Set(0, duration = 1000))
                    set(Set(0), repeat = 3)
                }
            }
            "Alex" -> WorkoutBuilder.workout {
                exercise("Front Press") {
                    warmup(xMin = 8, min = 16, med = 24, max = 30)
                    set(Set(30000, duration = 12000), repeat = 4)
                }
                exercise("Squat") {
                    assessmentTest(min = 30, med = 45, max = 60)
                }
                exercise("Shrugs") {
                    assessmentTest(min = 25, med = 35, max = 55)
                }
            }
            "Rob" -> WorkoutBuilder.workout {
                exercise("Shrugs") {
                    set(Set(0, duration = 15000, restTime = 10000))
                    set(Set(0, duration = 10000, restTime = 10000))
                    set(Set(0, duration = 5000, restTime = 15000))
                    set(Set(0, duration = 1000))
                    set(Set(0), repeat = 3)
                }
                exercise("Calf Lifts") {
                    set(Set(30000, duration = 10000, restTime = 10000))
                    set(Set(45000, duration = 5000, restTime = 15000))
                    set(Set(65000, duration = 1000))
                    set(Set(65000, duration = 12000), repeat = 2)
                    set(Set(65000, duration = 9000), repeat = 2)
                }
                exercise("Front Press") {
                    set(Set(15000, duration = 10000, restTime = 10000))
                    set(Set(22000, duration = 5000, restTime = 15000))
                    set(Set(35000, duration = 1000))
                    set(Set(35000, duration = 15000))
                    set(Set(35000, duration = 15000), repeat = 3)
                }
            }
            else -> return EntityMocks.WORKOUT
        }
    }
}