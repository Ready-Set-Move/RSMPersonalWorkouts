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

    fun assessmentTest(min: Int, med: Int, max: Int) {
        sets.addAll(setOf(
            Set(tractionGoal = min, duration = 15, restTime = 5),
            Set(tractionGoal = med, duration = 10, restTime = 15),
            Set(tractionGoal = max, duration = 5),
        ))
        set(Set(max), repeat = 4)
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