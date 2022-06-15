package com.readysetmove.personalworkouts.workout

interface IsWorkoutRepository {
    suspend fun fetchLatestWorkoutForUser(userId: String): Workout
}

class WorkoutRepository: IsWorkoutRepository {
    override suspend fun fetchLatestWorkoutForUser(userId: String): Workout {
        return when(userId) {
            "Flo" -> WorkoutBuilder.workout {
                exercise("Deadlift") {
                    set(Set(30000, duration = 15000, restTime = 5000))
                    set(Set(45000, duration = 10000, restTime = 10000))
                    set(Set(60000, duration = 5000, restTime = 15000))
                    set(Set(90000, duration = 1000))
                    set(Set(90000, duration = 12000), repeat = 4)
                }
                exercise("Drag Curls") {
                    set(Set(7000, duration = 15000, restTime = 5000))
                    set(Set(10000, duration = 10000, restTime = 10000))
                    set(Set(14000, duration = 5000, restTime = 15000))
                    set(Set(19000, duration = 1000))
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
            else -> return EntityMocks.WORKOUT
        }
    }
}