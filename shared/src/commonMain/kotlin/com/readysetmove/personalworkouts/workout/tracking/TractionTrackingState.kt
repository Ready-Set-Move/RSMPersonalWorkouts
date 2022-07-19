package com.readysetmove.personalworkouts.workout.tracking

import com.readysetmove.personalworkouts.device.Traction
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.workout.tracking.TractionTrackingState.*

interface CanPrepareTracking

sealed class TractionTrackingState: State {
    object TrackingNotStarted: TractionTrackingState(), CanPrepareTracking
    data class WaitingToTrackTraction(val tractionGoal: Long): TractionTrackingState()
    data class Tracking(val tractions: List<Traction>): TractionTrackingState()
    data class TractionsTracked(val tractions: List<Traction>): TractionTrackingState(), CanPrepareTracking
}

fun CanPrepareTracking.prepareTracking(tractionGoal: Long): WaitingToTrackTraction {
    return WaitingToTrackTraction(tractionGoal = tractionGoal)
}

fun WaitingToTrackTraction.startTracking(): Tracking {
    return Tracking(tractions = emptyList())
}

fun Tracking.addTraction(traction: Traction): Tracking {
    return copy(tractions = tractions.plus(traction))
}

fun Tracking.stopTracking(): TractionsTracked {
    return TractionsTracked(tractions = tractions)
}