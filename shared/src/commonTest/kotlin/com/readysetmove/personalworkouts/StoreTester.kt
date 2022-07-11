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
    private val testScheduler: TestCoroutineScheduler,
    private val testerMessage: String = "BASE TESTER",
    override val store: Store<StateType, ActionType, EffectType>,
)
    : IsStoreTester<StateType, ActionType, EffectType>, CoroutineScope by CoroutineScope(UnconfinedTestDispatcher(testScheduler)) {
    private val actions = mutableListOf<ActionType>()
    private val expects = mutableListOf<StateType>()
    private val verifys = mutableListOf<() -> Unit>()
    private var prepFunction: () -> Unit = {}
    private val steps = mutableListOf<IsStoreTester<StateType, ActionType, EffectType>>()

    override fun prepare(runPrepare: () -> Unit) {
        prepFunction = runPrepare
    }

    override fun dispatch(actionFun: () -> ActionType) {
        actions.add(actionFun())
    }

    override fun expect(stateFun: () -> StateType): StateType {
        val state = stateFun()
        expects.add(state)
        return state
    }

    override fun verifyMock(verifyFun: () -> Unit) {
        verifys.add(verifyFun)
    }

    override fun run(initialState: StateType?): StateType? {
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
        verifys.forEach {
            verify { it() }
        }
        if (expects.isNotEmpty()) {
            if (initialState != null) {
                // gathering always yields the initial state first so we may need to add it
                // if this is a step running after previous steps
                expects.add(0, initialState)
            }
            // TODO: enhance test failure output to to find errors faster (iterate over states?)
            assertEquals(
                expects,
                values,
                testerMessage,
            )
        }
        var lastState = if (expects.isNotEmpty()) expects.last() else null
        steps.forEach {
            lastState = it.run(lastState)
        }
        return lastState
    }

    override fun step(stepMessage: String, buildStep: IsStoreTester<StateType, ActionType, EffectType>.() -> Unit) {
        val stepTester = StoreTester(
            testScheduler = testScheduler,
            store = store,
            testerMessage = stepMessage,
        )
        stepTester.buildStep()
        steps.add(stepTester)
    }
}