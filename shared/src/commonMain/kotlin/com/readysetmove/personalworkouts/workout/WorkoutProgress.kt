package com.readysetmove.personalworkouts.workout

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

fun WorkoutProgress.activeSet(): Set {
    return activeExercise().sets[activeSetIndex]
}

private fun WorkoutProgress.forNextExercise(): WorkoutProgress {
    // at the end > jump back to beginning
    val nextExerciseIndex = if(atLastSetOfWorkout()) 0 else activeExerciseIndex + 1
    return copy(
        activeExerciseIndex = nextExerciseIndex,
        activeSetIndex = 0,
    )
}

fun WorkoutProgress.forNextStep(): WorkoutProgress {
    val activeExercise = activeExercise()
    val newProgress =
        // was this the last set of the exercise? If so finish the exercise...
        if (activeSet() === activeExercise.sets.last()) forNextExercise()
        // ...otherwise set active set to next set
        else copy(
            activeSetIndex = activeSetIndex + 1
        )
    Napier.d("Returning new progress: $newProgress from $this.")
    return newProgress
}

