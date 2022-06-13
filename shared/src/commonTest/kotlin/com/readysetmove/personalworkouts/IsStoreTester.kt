package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State

interface IsStoreTester<StateType: State, ActionType: Action, EffectType: Effect> {

    fun prepare(runPrepare: () -> Unit = {})

    fun dispatch(actionFun: () -> ActionType)

    fun expect(stateFun: () -> StateType)

    fun verifyMock(verifyFun: () -> Unit)

    fun run()
}