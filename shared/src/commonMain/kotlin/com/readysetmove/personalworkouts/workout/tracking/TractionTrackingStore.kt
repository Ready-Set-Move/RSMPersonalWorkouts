package com.readysetmove.personalworkouts.workout.tracking

import com.readysetmove.personalworkouts.IsTimestampProvider
import com.readysetmove.personalworkouts.device.DeviceState
import com.readysetmove.personalworkouts.device.IsDeviceStore
import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.Store
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressSideEffect
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressState
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressStore
import com.readysetmove.personalworkouts.workout.progress.startSetAction
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsStore
import com.readysetmove.personalworkouts.workout.tracking.TractionTrackingAction.*
import com.readysetmove.personalworkouts.workout.tracking.TractionTrackingState.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

sealed class TractionTrackingSideEffect: Effect

class TractionTrackingStore(
    private val deviceStore: IsDeviceStore,
    private val timestampProvider: IsTimestampProvider,
    private val workoutResultsStore: WorkoutResultsStore,
    private val workoutProgressStore: WorkoutProgressStore,
    mainDispatcher: CoroutineContext
):
    Store<TractionTrackingState, TractionTrackingAction, TractionTrackingSideEffect>,
    CoroutineScope by CoroutineScope(mainDispatcher)
{
    private val state = MutableStateFlow<TractionTrackingState>(TrackingNotStarted)
    private val sideEffect = MutableSharedFlow<TractionTrackingSideEffect>()
    private var trackingJob: Job? = null

    override fun observeState() = state
    override fun observeSideEffect() = sideEffect

    init {
        // initial listener on start of set
        launch {
            listenToNewSetActivated(TrackingNotStarted)
        }
    }

    override fun dispatch(action: TractionTrackingAction) {
        when(action) {
            is PrepareTracking -> {
                state.value = action.waitingToTrackTraction
                launch {
                    waitToHitStartTrackingGoal(action.waitingToTrackTraction)
                }
                launch {
                    listenForTractionGoalReached(tractionGoal = action.waitingToTrackTraction.tractionGoal)
                }
            }
            is StartTracking -> {
                state.value = action.tracking
                trackingJob = launch {
                    trackTractions(
                        tracking = action.tracking,
                        startedAt = timestampProvider.getTimeMillis(),
                    )
                }.apply {
                    invokeOnCompletion {
                        dispatch(action.tracking.transitionToTractionsTrackedAction())
                    }
                }
                launch {
                    listenToWorkFinished(tracking = action.tracking)
                }
            }
            is TransitionToTractionsTracked -> {
                state.value = action.tractionsTracked
                // TODO: split tracked data in: ramp up | work
                //  also calculate min | max | median of workout phase
                workoutResultsStore.pendingTractions = action.tractionsTracked.tractions
                launch {
                    listenToNewSetActivated(action.tractionsTracked)
                }
            }
            is StopTracking -> {
                trackingJob?.cancel()
            }
        }
    }

    private suspend fun waitToHitStartTrackingGoal(waitingToTrackTraction: WaitingToTrackTraction) {
        waitingToTrackTraction.tractionGoal.let {
            deviceStore.observeState().first { deviceState ->
                deviceState.reachedStartTrackingGoal(waitingToTrackTraction.tractionGoal)
            }
        }
        dispatch(waitingToTrackTraction.startTrackingAction())
    }

    private suspend fun listenForTractionGoalReached(tractionGoal: Long) {
        // start set as soon as we have relevant traction delivered
        deviceStore.observeState().first { deviceState ->
            // TODO: what if we don't hit the minimal goal?
            deviceState.reachedTractionGoal(tractionGoal)
        }
        // make sure we land at the correct workout state before continuing
        val workoutProgressState = workoutProgressStore.observeState()
            .filterIsInstance<WorkoutProgressState.WaitingToStartSet>()
            .first()
        workoutProgressStore.dispatch(workoutProgressState.startSetAction())
    }

    private suspend fun trackTractions(tracking: Tracking, startedAt: Long) {
        deviceStore.observeState().collect {
            state.value = tracking.addTraction(Traction(
                timestamp = timestampProvider.getTimeMillis() - startedAt,
                value = it.traction,
            ))
        }
    }

    private suspend fun listenToNewSetActivated(canPrepareTracking: CanPrepareTracking) {
        val newSetActivatedEffect = workoutProgressStore.observeSideEffect()
            .filterIsInstance<WorkoutProgressSideEffect.NewSetActivated>()
            .first()
        dispatch(canPrepareTracking.prepareTrackingAction(
            tractionGoal = newSetActivatedEffect.tractionGoal
        ))
    }

    private suspend fun listenToWorkFinished(tracking: Tracking) {
        workoutProgressStore.observeSideEffect()
            .filterIsInstance<WorkoutProgressSideEffect.WorkFinished>()
            .first()
        dispatch(tracking.stopTrackingAction())
    }
}

// for sets without traction goal defined (assessment)
fun DeviceState.reachedStartTrackingGoal(tractionGoal: Long) =
    traction*1000 >= if(tractionGoal>0) tractionGoal*.2f else 2f
fun DeviceState.reachedTractionGoal(tractionGoal: Long) =
    traction*1000 >= if(tractionGoal>0) tractionGoal*.8f else 5f