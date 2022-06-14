package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store

interface IsStoreTester<StateType: State, ActionType: Action, EffectType: Effect> {
    val store: Store<StateType, ActionType, EffectType>

    fun step(stepMessage: String, buildStep: IsStoreTester<StateType, ActionType, EffectType>.() -> Unit)

    fun prepare(runPrepare: () -> Unit = {})

    fun dispatch(actionFun: () -> ActionType)

    fun expect(stateFun: () -> StateType): StateType

    fun verifyMock(verifyFun: () -> Unit)

    fun run(initialState: StateType? = null): StateType?
}