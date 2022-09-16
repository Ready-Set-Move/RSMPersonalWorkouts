package com.readysetmove.personalworkouts.workout

class ExerciseBuilder(val name: String, val comment: String, val position: String) {
    private val sets = mutableListOf<Set>()

    fun warmup(min: Int, med: Int, max: Int, xMin: Int? = null) {
        if (xMin != null) {
            sets.add(Set(tractionGoal = xMin, duration = 15, 5))
        }
        sets.addAll(setOf(
            Set(tractionGoal = min, duration = 10, restTime = 10),
            Set(tractionGoal = med, duration = 5, restTime = 15),
            Set(tractionGoal = max, duration = 1),
        ))
    }

    fun assessmentTest(min: Int = 0, med: Int = 0, max: Int = 0, test: Int = 0) {
        sets.addAll(listOf(
            Set(tractionGoal = min, duration = 15, restTime = 5),
            Set(tractionGoal = med, duration = 10, restTime = 15),
            Set(tractionGoal = med, duration = 10, restTime = 15),
            Set(tractionGoal = max, duration = 5, restTime = 20),
            Set(tractionGoal = max, duration = 5, restTime = 30),
        ))
        set(Set(test), repeat = 3)
    }

    fun set(set: Set, repeat: Int = 1) {
        (1..repeat).forEach { _ ->
            sets.add(set.copy())
        }
    }

    fun build(): Exercise {
        return Exercise(
            name = name,
            comment = comment,
            sets = sets,
            position = position,
        )
    }

    companion object {
        fun exercise(
            name: String = "Testcercise",
            comment: String = "Some comment",
            position: String = "Zee position",
            init: ExerciseBuilder.() -> Unit,
        ): Exercise {
            val builder = ExerciseBuilder(
                name = name,
                comment = comment,
                position = position,
            )
            builder.init()
            return builder.build()
        }
    }
}

class WorkoutBuilder(private val id: String, val comment: String) {
    private val exercises = mutableListOf<ExerciseBuilder>()

    companion object {
        fun alex(): Pair<String, Workout> {
            return Pair("CoHbkbOvIUYg4y2NSheGQFtiV2P2", workout(id = "2022-08-22") {
                exercise("Overhead Press", position = "@home: 22 | @studio: holds 16") {
                    warmup(10,15,20, 5)
                    set(Set(20, 15, 30))
                    set(Set(20, 12, 30), repeat = 3)
                }
                exercise("Shrugs", position = "@home: 6 | @studio: holds 0") {
                    warmup(42,60,85)
                    set(Set(85, 15, 30))
                    set(Set(85, 12, 30), repeat = 3)
                }
                exercise("Squat", position = "@home: 5 | @studio: holds direct") {
                    warmup(54,80,107)
                    set(Set(107, 15, 30))
                    set(Set(107, 12, 30), repeat = 3)
                }
                exercise("Curls", position = "@home: ? | @studio: ?") {
                    warmup(12,18,25, 8)
                    set(Set(25, 9, 30), repeat = 4)
                }
            })
        }

        fun marko(): Pair<String, Workout> {
            return Pair("pGCpK7skdZbDF7THT5XoIk3y25X2", workout(id = "2022-08-25") {
                exercise("Squat", position = "@home: 5 | @studio: holds direct") {
                    assessmentTest(15, 30, 45)
                }
                exercise("Curls", position = "@home: ? | @studio: ?") {
                    assessmentTest(5, 8, 10)
                }
            })
        }

        fun flo(): Pair<String, Workout> {
            return Pair("6QpQhtAwRZVd7CsIQeakb2i3V9k1", workout(id = "2022-09-09") {
                exercise("Deadlift", position = "rings @ 0") {
                    warmup(45,60,90, 30)
                    set(Set(90, 6))
                    set(Set(90, 20), 3)
                }
            })
        }

        fun jonas(): Pair<String, Workout> {
            return Pair("rWvysPMNmmVtL7lNonOcHWF1V223", workout(id = "2022-09-02") {
                exercise("Squat", position = "holds direct") {
                    assessmentTest(test = 30)
                }
                exercise("Overhead Press", position = "?") {
                    assessmentTest(test = 10)
                }
            })
        }

        fun mario(): Pair<String, Workout> {
            return Pair("h9bxMY1414Mr9RoLxNq156cLTnF2", workout(id = "2022-08-29") {
                exercise("Squat", position = "@home: 5 | @studio: holds direct") {
                    assessmentTest(15, 30, 45)
                }
                exercise("Rows", position = "@home: ? | @studio: ?") {
                    assessmentTest(5, 10, 15)
                }
            })
        }

        fun jose(): Pair<String, Workout> {
            return Pair("MmLQZ68HuNVg6XcbvyffsGuIzmz1", workout(id = "2022-09-09") {
                exercise("Front Raise", position = "@home: 4 | @studio: 1") {
                    warmup(12,18,25, 8)
                    set(Set(25, 6))
                    set(Set(25, 20), 3)
                }
                exercise("Shrugs", position = "@home: 6 | @studio: 3") {
                    warmup(30,45,58)
                    set(Set(58, 6))
                    set(Set(58, 20), 3)
                }
                exercise("Squat", position = "@home: 6 | @studio: 3") {
                    warmup(85,120,175)
                    set(Set(175, 6))
                    set(Set(175, 20), 3)
                }
                exercise("Curls", position = "@home: 9 | @studio: 6") {
                    warmup(15,22,30, 8)
                    set(Set(30, 6))
                    set(Set(30, 20), 3)
                }
            })
        }

        fun peter(): Pair<String, Workout> {
            return Pair("ReWiclRDjLa1ofJSYexsUXHEIh82", workout(id = "2022-09-09") {
                exercise("Squat", position = "?") {
                    warmup(30,40,50)
                    set(Set(50, 6))
                    set(Set(50, 20), 3)
                }
                exercise("Seated Rows", position = "?") {
                    warmup(10,15, 20)
                    set(Set(20, 6))
                    set(Set(20, 20), 3)
                }
//                exercise("Chest Press", position = "?") {
//                    warmup(30,45,58)
//                    set(Set(58, 6))
//                    set(Set(58, 20), 3)
//                }
            })
        }

        fun workout(
            id: String = "TEST_WORKOUT",
            comment: String = "$id TEST_COMMENT",
            init: WorkoutBuilder.() -> Unit
        ): Workout {
            val builder = WorkoutBuilder(
                id = id,
                comment = comment,
            )
            builder.init()
            return builder.build()
        }
    }

    fun exercise(
        name: String = "TESTCERCISE",
        comment: String = "$name TEST_COMMENT",
        position: String = "0",
        init: ExerciseBuilder.() -> Unit
    ): ExerciseBuilder {
        val builder = ExerciseBuilder(
            name = name,
            comment = comment,
            position = position
        )
        exercises.add(builder)
        builder.init()
        return builder
    }

    private fun build(): Workout {
        val builtExercises = mutableListOf<Exercise>()
        exercises.forEach {
            builtExercises.add(it.build())
        }
        return Workout(
            id = id,
            comment = comment,
            exercises = builtExercises,
        )
    }
}