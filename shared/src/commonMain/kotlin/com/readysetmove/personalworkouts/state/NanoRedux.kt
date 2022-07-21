package com.readysetmove.personalworkouts.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface State
interface Action
interface Effect

interface Store<S : State, A : Action, E : Effect>: SimpleStore<S, A> {
    fun observeSideEffect(): Flow<E>
}

interface SimpleStore<S : State, A : Action> {
    fun observeState(): StateFlow<S>
    fun dispatch(action: A)
}