package com.readysetmove.personalworkouts.workout

import com.readysetmove.personalworkouts.IsTimestampProvider
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class WorkoutProgress(
    val workout: Workout,
    val activeExerciseIndex: Int = 0,
    val activeSetIndex: Int = 0,
)

data class WorkoutState(
    val workoutProgress: WorkoutProgress? = null,
    val timeToWork: Long = 0,
    val timeToRest: Long = 0,
    val tractionGoal: Long? = null,
    val durationGoal: Long? = null,
    val working: Boolean = false,
    val startTime: Long = 0,
) : State

fun WorkoutProgress.activeExercise(): Exercise {
    return workout.exercises[activeExerciseIndex]
}

fun WorkoutProgress.activeSet(): Set {
    return activeExercise().sets[activeSetIndex]
}

sealed class WorkoutAction: Action {
    data class StartWorkout(val workout: Workout): WorkoutAction()
    object FinishSet: WorkoutAction()
    object FinishExercise: WorkoutAction()
    object StartSet: WorkoutAction()
    object StartRest: WorkoutAction()
    data class SetTractionGoal(val tractionGoal: Long): WorkoutAction()
    data class SetDurationGoal(val durationGoal: Long): WorkoutAction()
}

sealed class WorkoutExceptions : Exception() {
    data class EmptyWorkoutException(val workout: Workout): WorkoutExceptions()
    data class EmptyExerciseException(
        val workout: Workout,
        val exercise: Exercise,
    ): WorkoutExceptions()
}

sealed class WorkoutStoreExceptions : Exception() {
    object WorkoutNotSetException: WorkoutStoreExceptions()
    object NoTractionGoalAtStartOfSetException: WorkoutExceptions()
    object NoDurationGoalAtStartOfSetException: WorkoutExceptions()
}

sealed class WorkoutSideEffect : Effect {
    data class Error(val error: Exception) : WorkoutSideEffect()
    object NewWorkoutStarted : WorkoutSideEffect()
    data class SetFinished(val workoutProgress: WorkoutProgress, val tractionGoal: Long) : WorkoutSideEffect()
    object WorkoutFinished : WorkoutSideEffect()
}

class WorkoutStore(
    initialState: WorkoutState = WorkoutState(),
    private val timestampProvider: IsTimestampProvider,
    private val mainDispatcher: CoroutineContext,
):
    Store<WorkoutState, WorkoutAction, WorkoutSideEffect>,
    CoroutineScope by CoroutineScope(mainDispatcher) {

    private val state = MutableStateFlow(initialState)
    private val sideEffect = MutableSharedFlow<WorkoutSideEffect>()
    override fun observeState(): StateFlow<WorkoutState> = state
    override fun observeSideEffect(): Flow<WorkoutSideEffect> = sideEffect

    private fun launchError(error: WorkoutSideEffect.Error) {
        launch {
            sideEffect.emit(error)
        }
    }

    companion object {
        const val TICKS = 10L
    }

    override fun dispatch(action: WorkoutAction) {
        val check4Workout = {
            if (state.value.workoutProgress == null) {
                launchError(WorkoutSideEffect.Error(WorkoutStoreExceptions.WorkoutNotSetException))
            }
            state.value.workoutProgress
        }
        when(action) {
            is WorkoutAction.StartWorkout -> {
                if (!action.workout.isValid(launchError = this::launchError)) return

                val firstSet = action.workout.exercises.first().sets.first()
                state.value = WorkoutState(
                    workoutProgress = WorkoutProgress(
                        workout = action.workout,
                    ),
                    timeToWork = firstSet.duration,
                    timeToRest = 0,
                    tractionGoal = firstSet.tractionGoal,
                    durationGoal = firstSet.duration,
                )
                launch {
                    sideEffect.emit(WorkoutSideEffect.NewWorkoutStarted)
                }
            }
            is WorkoutAction.FinishExercise -> {
                check4Workout()?.let { workoutProgress ->
                    // already last exercise?
                    if (workoutProgress.activeExercise() == workoutProgress.workout.exercises.last()) {
                        launch {
                            sideEffect.emit(WorkoutSideEffect.WorkoutFinished)
                        }
                        return
                    }

                    // starting next exercise
                    val nextExerciseIndex = workoutProgress.activeExerciseIndex + 1
                    val nextSet = workoutProgress.workout.exercises[nextExerciseIndex].sets.first()
                    state.value = state.value.copy(
                        workoutProgress = workoutProgress.copy(
                            activeExerciseIndex = nextExerciseIndex,
                        ),
                        timeToRest = 0,
                        timeToWork = nextSet.duration,
                        tractionGoal = nextSet.tractionGoal,
                        durationGoal = nextSet.duration,
                    )
                }
            }
            is WorkoutAction.StartSet -> {
                check4Workout()?.let { workoutProgress ->
                    val currentTractionGoal = state.value.tractionGoal
                    val currentDurationGoal = state.value.durationGoal
                    if (currentTractionGoal == null) {
                        launchError(WorkoutSideEffect.Error(WorkoutStoreExceptions.NoTractionGoalAtStartOfSetException))
                        return
                    }
                    if (currentDurationGoal == null) {
                        launchError(WorkoutSideEffect.Error(WorkoutStoreExceptions.NoDurationGoalAtStartOfSetException))
                        return
                    }
                    state.value = state.value.copy(
                        working = true,
                        startTime = timestampProvider.getTimeMillis()
                    )
                    // Start the set and count down time
                    val workTimeCountdown = launch {
                        while (state.value.timeToWork > 0) {
                            delay(TICKS)
                            val timeDone = timestampProvider.getTimeMillis() - state.value.startTime
                            state.value = state.value.copy(timeToWork = currentDurationGoal - timeDone)
                        }
                    }
                    workTimeCountdown.invokeOnCompletion {
                        launch {
                            sideEffect.emit(WorkoutSideEffect.SetFinished(
                                workoutProgress = workoutProgress,
                                tractionGoal = currentTractionGoal,
                                // TODO: emit duration goal (set?)
                            ))
                        }
                        dispatch(WorkoutAction.StartRest)
                        state.value = state.value.copy(working = false)
                    }
                }
            }
            is WorkoutAction.StartRest -> {
                check4Workout()?.let { workoutProgress ->
                    state.value = state.value.copy(
                        timeToRest = workoutProgress.activeSet().restTime,
                        timeToWork = 0,
                        startTime = timestampProvider.getTimeMillis(),
                    )
                    val restTimeCounter = launch {
                        while (state.value.timeToRest > 0) {
                            delay(TICKS)
                            val timeDone = timestampProvider.getTimeMillis() - state.value.startTime
                            state.value = state.value.copy(timeToRest = workoutProgress.activeSet().restTime - timeDone)
                        }
                    }
                    restTimeCounter.invokeOnCompletion {
                        dispatch(WorkoutAction.FinishSet)
                    }
                }
            }
            is WorkoutAction.FinishSet -> {
                check4Workout()?.let { workoutProgress ->
                    // was this the last set of the exercise? If so finish the exercise...
                    if (workoutProgress.activeSet() == workoutProgress.activeExercise().sets.last())
                        dispatch(WorkoutAction.FinishExercise)
                    // ...otherwise set active set to next set
                    else
                        state.value.copy(
                            workoutProgress = workoutProgress.copy(
                                activeSetIndex = workoutProgress.activeSetIndex + 1
                            )
                        )
                }
            }
            is WorkoutAction.SetDurationGoal -> {
                if (state.value.working) return

                state.value = state.value.copy(durationGoal = action.durationGoal)
            }
            is WorkoutAction.SetTractionGoal -> {
                if (state.value.working) return

                state.value = state.value.copy(tractionGoal = action.tractionGoal)
            }
        }
    }
}

fun Workout.isValid(launchError: (WorkoutSideEffect.Error) -> Unit): Boolean {
    if (exercises.isEmpty()) {
        launchError(WorkoutSideEffect.Error(
            WorkoutExceptions.EmptyWorkoutException(this)
        ))
        return false
    }
    exercises.forEach { exercise ->
        if (exercise.sets.isEmpty()) {
            launchError(WorkoutSideEffect.Error(
                WorkoutExceptions.EmptyExerciseException(
                    workout = this,
                    exercise = exercise,
                )
            ))
            return false
        }
    }
    return true
}

