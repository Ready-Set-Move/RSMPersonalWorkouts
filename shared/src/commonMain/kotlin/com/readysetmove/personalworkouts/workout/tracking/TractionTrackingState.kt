package com.readysetmove.personalworkouts.workout.tracking

import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.workout.tracking.TractionTrackingState.*

sealed class TractionTrackingAction: Action {
    data class PrepareTracking(val waitingToTrackTraction: WaitingToTrackTraction): TractionTrackingAction()
    data class StartTracking(val tracking: Tracking): TractionTrackingAction()
    object StopTracking: TractionTrackingAction()
    data class TransitionToTractionsTracked(val tractionsTracked: TractionsTracked): TractionTrackingAction()
}

interface CanPrepareTracking

sealed class TractionTrackingState: State {
    object TrackingNotStarted: TractionTrackingState(), CanPrepareTracking
    data class WaitingToTrackTraction(val tractionGoal: Long): TractionTrackingState()
    data class Tracking(val tractions: List<Traction>): TractionTrackingState()
    data class TractionsTracked(val tractions: List<Traction>): TractionTrackingState(), CanPrepareTracking
}

fun CanPrepareTracking.prepareTrackingAction(tractionGoal: Long): TractionTrackingAction.PrepareTracking {
    return TractionTrackingAction.PrepareTracking(
        waitingToTrackTraction = WaitingToTrackTraction(tractionGoal = tractionGoal)
    )
}

fun WaitingToTrackTraction.startTrackingAction(): TractionTrackingAction.StartTracking {
    return TractionTrackingAction.StartTracking(tracking = Tracking(tractions = emptyList()))
}

fun Tracking.addTraction(traction: Traction): Tracking {
    return copy(tractions = tractions.plus(traction))
}

fun Tracking.stopTrackingAction(): TractionTrackingAction.StopTracking {
    return TractionTrackingAction.StopTracking
}
fun Tracking.transitionToTractionsTrackedAction(): TractionTrackingAction.TransitionToTractionsTracked {
    return TractionTrackingAction.TransitionToTractionsTracked(tractionsTracked = TractionsTracked(tractions = tractions))
}