package com.readysetmove.personalworkouts.workout.progress

import com.readysetmove.personalworkouts.workout.Exercise
import com.readysetmove.personalworkouts.workout.Set
import com.readysetmove.personalworkouts.workout.Workout
import io.github.aakira.napier.Napier

data class WorkoutProgress(
    val workout: Workout,
    val activeExerciseIndex: Int = 0,
    val activeSetIndex: Int = 0,
)

fun WorkoutProgress.activeExercise(): Exercise {
    return workout.exercises[activeExerciseIndex]
}

fun WorkoutProgress?.atLastSetOfWorkout(): Boolean {
    if (this == null) return false
    return activeSet() === workout.exercises.last().sets.last()
}

fun WorkoutProgress?.atLastSetOfExercise(): Boolean {
    if (this == null) return false
    return activeSet() === activeExercise().sets.last()
}

fun WorkoutProgress.activeSet(): Set {
    return activeExercise().sets[activeSetIndex]
}

fun WorkoutProgress.forNextExercise(): WorkoutProgress {
    // at the end > jump back to beginning
    val nextExerciseIndex = if(atLastSetOfWorkout()) 0 else activeExerciseIndex + 1
    return copy(
        activeExerciseIndex = nextExerciseIndex,
        activeSetIndex = 0,
    )
}

fun WorkoutProgress.forNextSet(): WorkoutProgress {
    if (activeSet() === activeExercise().sets.last()) {
        Napier.d("Already at last set, can't progress inside exercise from $this.")
        return this
    }

    return copy(
        activeSetIndex = activeSetIndex + 1
    )
}