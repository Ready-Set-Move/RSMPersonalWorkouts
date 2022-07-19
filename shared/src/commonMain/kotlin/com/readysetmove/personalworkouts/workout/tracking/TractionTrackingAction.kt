package com.readysetmove.personalworkouts.workout.tracking

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.workout.tracking.TractionTrackingAction.StartTracking
import com.readysetmove.personalworkouts.workout.tracking.TractionTrackingAction.StopTracking
import com.readysetmove.personalworkouts.workout.tracking.TractionTrackingState.*

sealed class TractionTrackingAction: Action {
    data class PrepareTracking(val waitingToTrackTraction: WaitingToTrackTraction): TractionTrackingAction()
    data class StartTracking(val tracking: Tracking): TractionTrackingAction()
    data class StopTracking(val tractionsTracked: TractionsTracked): TractionTrackingAction()
    data class TransitionToTractionsTracked(val tractionsTracked: TractionsTracked): TractionTrackingAction()
}

fun CanPrepareTracking.prepareTrackingAction(tractionGoal: Long): TractionTrackingAction.PrepareTracking {
    return TractionTrackingAction.PrepareTracking(
        waitingToTrackTraction = prepareTracking(tractionGoal)
    )
}

fun WaitingToTrackTraction.startTrackingAction(): StartTracking {
    return StartTracking(tracking = startTracking())
}

fun Tracking.stopTrackingAction(): StopTracking {
    return StopTracking(tractionsTracked = stopTracking())
}