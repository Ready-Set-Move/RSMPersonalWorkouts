package com.readysetmove.personalworkouts.workout.results

import com.readysetmove.personalworkouts.state.SimpleStore
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsAction.*
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsState.NoResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class WorkoutResultsStore(
    private val workoutResultsRepository: IsWorkoutResultsRepository,
    mainDispatcher: CoroutineContext,
):
    SimpleStore<WorkoutResultsState, WorkoutResultsAction>,
    CoroutineScope by CoroutineScope(mainDispatcher)
{
    private val state = MutableStateFlow<WorkoutResultsState>(NoResults)
    override fun observeState() = state

    override fun dispatch(action: WorkoutResultsAction) {
        when(action) {
            is SetTractions -> {
                state.value = action.waitingToRateSet
            }
            is RateSet -> {
                state.value = action.waitingForTractions
                launch {
                    workoutResultsRepository.storeResults(action.waitingForTractions.workoutResults)
                }
            }
            is RateExercise -> {
                state.value = action.waitingForTractions
                launch {
                    workoutResultsRepository.storeResults(action.waitingForTractions.workoutResults)
                }
            }
        }
    }
}