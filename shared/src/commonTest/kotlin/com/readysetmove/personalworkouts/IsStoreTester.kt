package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.SimpleStore
import com.readysetmove.personalworkouts.state.State

interface IsStoreTester<StateType: State, ActionType: Action> {
    val store: SimpleStore<StateType, ActionType>

    fun step(stepMessage: String, buildStep: IsStoreTester<StateType, ActionType>.() -> Unit)

    fun prepare(runPrepare: () -> Unit = {})

    fun dispatch(actionFun: () -> ActionType)

    fun <IncomingState: StateType>expect(stateFun: () -> IncomingState): IncomingState

    fun verifyMock(verifyFun: () -> Unit)

    fun run(initialState: StateType? = null): StateType?
}