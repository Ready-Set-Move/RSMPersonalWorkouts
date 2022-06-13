package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.assertEquals

class StoreTester<StateType: State, ActionType: Action, EffectType: Effect>(
    val store: Store<StateType, ActionType, EffectType>,
    private val testScheduler: TestCoroutineScheduler,
)
    : IsStoreTester<StateType, ActionType, EffectType>, CoroutineScope by CoroutineScope(UnconfinedTestDispatcher(testScheduler)) {
    private val actions = mutableListOf<ActionType>()
    private val expects = mutableListOf<StateType>()
    private val verifys = mutableListOf<() -> Unit>()
    private var prepFunction: () -> Unit = {}

    override fun prepare(runPrepare: () -> Unit) {
        prepFunction = runPrepare
    }

    override fun dispatch(actionFun: () -> ActionType) {
        actions.add(actionFun())
    }

    override fun expect(stateFun: () -> StateType) {
        expects.add(stateFun())
    }

    override fun verifyMock(verifyFun: () -> Unit) {
        verifys.add(verifyFun)
    }

    override fun run() {
        val values = mutableListOf<StateType>()
        val gatherStatesJob = if (expects.isNotEmpty()) launch {
            store.observeState().toList(values)
        } else null
        prepFunction()

        actions.forEach {
            store.dispatch(it)
        }
        testScheduler.advanceUntilIdle()
        gatherStatesJob?.cancel()
        assertEquals(
            expects,
            values
        )
        verifys.forEach {
            verify { it() }
        }
    }
}