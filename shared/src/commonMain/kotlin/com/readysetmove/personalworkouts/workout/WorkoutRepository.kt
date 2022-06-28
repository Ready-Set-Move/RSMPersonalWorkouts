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
                    set(Set(90000, duration = 15000))
                    set(Set(90000, duration = 12000), repeat = 3)
                }
                exercise("Drag Curls") {
                    warmup(xMin = 7, min = 10, med = 14, max = 19)
                    set(Set(19000, duration = 6000), repeat = 5)
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
            "robert.pazurek@gmail.com" -> WorkoutBuilder.workout {
                exercise("Shrugs") {
                    warmup(xMin = 30, min = 50, med = 75, max = 100)
                    set(Set(100000), repeat = 6)
                }
                exercise("Calf Lifts") {
                    warmup(min = 30, med = 45, max = 65)
                    set(Set(65000, duration = 15000), repeat = 1)
                    set(Set(65000, duration = 12000), repeat = 3)
                }
                exercise("Front Press") {
                    warmup(min = 25, med = 40, max = 55)
                    set(Set(55000), repeat = 4)
                }
                exercise("Rotator Cuff Extensions") {
                    warmup(xMin = 5, min = 5, med = 8, max = 11)
                    set(Set(11000), repeat = 4)
                }
            }
            else -> return EntityMocks.WORKOUT
        }
    }
}