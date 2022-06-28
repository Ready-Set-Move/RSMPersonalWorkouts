package com.readysetmove.personalworkouts.workout

class ExerciseBuilder(val name: String, val comment: String) {
    private val sets = mutableListOf<Set>()

    fun warmup(min: Long, med: Long, max: Long, xMin: Long? = null) {
        if (xMin != null) {
            sets.add(Set(tractionGoal = xMin*1000, duration = 15000, 5000))
        }
        sets.addAll(setOf(
            Set(tractionGoal = min*1000, duration = 10000, restTime = 10000),
            Set(tractionGoal = med*1000, duration = 5000, restTime = 15000),
            Set(tractionGoal = max*1000, duration = 1000),
        ))
    }

    fun assessmentTest(min: Long, med: Long, max: Long) {
        sets.addAll(setOf(
            Set(tractionGoal = min*1000, duration = 15000, restTime = 5000),
            Set(tractionGoal = med*1000, duration = 10000, restTime = 15000),
            Set(tractionGoal = max*1000, duration = 5000),
        ))
        set(Set(max*1000), repeat = 4)
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
        )
    }
}

class WorkoutBuilder(private val id: String, val comment: String) {
    val exercises = mutableListOf<ExerciseBuilder>()

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
        init: ExerciseBuilder.() -> Unit
    ): ExerciseBuilder {
        val builder = ExerciseBuilder(
            name = name,
            comment = comment,
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