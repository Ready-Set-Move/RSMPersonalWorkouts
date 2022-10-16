package com.readysetmove.personalworkouts.workout

class ExerciseBuilder(val name: String, val comment: String, val position: String) {
    private val sets = mutableListOf<Set>()

    fun warmup(min: Int, med: Int, max: Int, xMin: Int? = null) {
        if (xMin != null) {
            sets.add(Set(tractionGoal = xMin, duration = 15, 5))
        }
        sets.addAll(setOf(
            Set(tractionGoal = min, duration = 12, restTime = 10),
            Set(tractionGoal = med, duration = 7, restTime = 15),
            Set(tractionGoal = max, duration = 3),
        ))
    }

    fun longWarmup(min: Int, med: Int) {
        sets.addAll(setOf(
            Set(tractionGoal = min, duration = 10, restTime = 5),
            Set(tractionGoal = min, duration = 10, restTime = 5),
            Set(tractionGoal = med, duration = 5, restTime = 15),
            Set(tractionGoal = med, duration = 5),
        ))
    }

    fun assessmentWarmup(min: Int, med: Int, max: Int) {
        sets.addAll(listOf(
            Set(tractionGoal = min, duration = 15, restTime = 5),
            Set(tractionGoal = med, duration = 10, restTime = 10),
            Set(tractionGoal = med, duration = 10, restTime = 10),
            Set(tractionGoal = max, duration = 5, restTime = 15),
            Set(tractionGoal = max, duration = 5),
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
        set(Set(test, duration = 4), repeat = 3)
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
        fun priya(): Pair<String, Workout> {
            return Pair("QetLWHUtRoZFpLufMEZxIvsoqzT2", workout(id = "2022-09-29") {
                exercise("Seated Rows", position = "Holds direct") {
                    warmup(12, 17, 20, 8)
                    set(Set(17, 22), 3)
                }
                exercise("Leg Raises", position = "Flat Bench direct") {
                    warmup(3, 4, 5, xMin = 2)
                    set(Set(4, 22), 3)
                }
            })
        }

        fun peter(): Pair<String, Workout> {
            return Pair("JmvGz1oeEwMq9nVj7GdtHX3L8mJ2", workout(id = "2022-09-29") {
                exercise("Seated Rows", position = "Untergriff Bar @2")     {
                    warmup(22,32, 40)
                    set(Set(32, 42), 3)
                }
                exercise("Bench Leg Raises", position = "2 Fußschlaufen direkt Platte", doubleSided = true) {
                    warmup(6,11, 14)
                    set(Set(11, 37), 3)
                }
                exercise("Chest Press", position = "Bar @15") {
                    warmup(12,20, 28)
                    set(Set(20, 32), 3)
                }
            })
        }

        fun flo(): Pair<String, Workout> {
            return Pair("6QpQhtAwRZVd7CsIQeakb2i3V9k1", workout(id = "2022-09-28") {
                exercise("Deadlift", position = "rings @ 0") {
                    warmup(75,105,125, 50)
                    set(Set(105, 35), 3)
                }
                exercise("Wall Crunches", position = "Bar direct | Lochraster 15 von unten") {
                    warmup(12,16,25, 8)
                    set(Set(16, 20), 3)
                }
            })
        }

        fun jonas(): Pair<String, Workout> {
            return Pair("7QdxrrDqBKeHCmyWtZs3Bq3buY03", workout(id = "2022-09-02") {
                exercise("Standing Rows", position = "? @ ?") {
                    warmup(8, 12, 15, 6)
                    set(Set(12, 6), 3)
                }
                exercise("Front Squat", position = "bar @ 8") {
                    warmup(12, 20, 25)
                    set(Set(20, 26, 60), 3)
                }
            })
        }

        fun rob(): Pair<String, Workout> {
            return Pair("KjofQw7eUQdv2W7Bm6FcM5Lvbj33", workout(id = "2022-09-27") {
                exercise("Chest Press", position = "Ringe | studio@12 | home@14", comment = "max: 59,5") {
                    warmup(15, 20, 30)
                    set(Set(30, 30), 3)
                }
                exercise("Leg Raises Left", position = "Fußschlaufe direkt an Wand | Standfuß vor Zugfuß | Hüfte nach vorn strecken", comment = "max: ~28 eher runter auf 13kg load") {
                    warmup(8, 10, 13)
                    set(Set(13, 30), 3)
                }
                exercise("Leg Raises Right", position = "Fußschlaufe direkt an Wand | Standfuß vor Zugfuß | Hüfte nach vorn strecken") {
                    warmup(8, 10, 13)
                    set(Set(13, 30), 3)
                }
                exercise("Curls", position = "Ringe | studio@6 | home@8", comment = "max: 39.2") {
                    warmup(10, 15, 19)
                    set(Set(19, 30), 3)
                }
            })
        }

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

        fun magda(): Pair<String, Workout> {
            return Pair("jpQYQ09AvXQ7vaXW8NZyLITVtCL2", workout(id = "2022-09-23") {
                exercise("Leg Raises Left", position = "Fußschlaufe direkt an Wand") {
                    assessmentWarmup(5, 8, 10)
                    set(Set(10, 20), 3)
                }
                exercise("Leg Raises Right", position = "Fußschlaufe direkt an Wand") {
                    assessmentWarmup(5, 8, 10)
                    set(Set(10, 20), 3)
                }
                exercise("Overhead Press", position = "Holds @14") {
                    assessmentWarmup(5, 6, 7)
                    set(Set(7, 20), 3)
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
                exercise("Front Raise", position = "@home: 10 | @studio: 7") {
                    warmup(5,10,12)
                    set(Set(10, 32), 3)
                }
                exercise("Squat", position = "@home: 11 | @studio: 8") {
                    warmup(35,62,80)
                    set(Set(62, 35), 3)
                }
                exercise("Shrugs", position = "@home: 6 | @studio: 3") {
                    warmup(30,52,65)
                    set(Set(52, 35), 3)
                }
                exercise("Curls", position = "@home: 9 | @studio: 6") {
                    warmup(15,22,30)
                    set(Set(22, 32), 3)
                }
            })
        }

        fun test(): Pair<String, Workout> {
            return Pair("akmgotyWSNUxYIfEfqUOA9trEDv1", workout(id = "2022-09-09") {
                exercise("Shrugs", position = "@home: 6 | @studio: 3") {
                    warmup(25,35,50)
                    set(Set(50, 20), 3)
                }
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
        doubleSided: Boolean = false,
        init: ExerciseBuilder.() -> Unit
    ): ExerciseBuilder {
        val builder = ExerciseBuilder(
            name = name,
            comment = comment,
            position = position
        )
        exercises.add(builder)
        if (doubleSided) exercises.add(builder)
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