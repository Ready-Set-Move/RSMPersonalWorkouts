package com.readysetmove.personalworkouts.workout

class ExerciseBuilder(val name: String, val comment: String) {
    val sets = mutableListOf<Set>()
    fun set(set: Set) {
        sets.add(set)
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