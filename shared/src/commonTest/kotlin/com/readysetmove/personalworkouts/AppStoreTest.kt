package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.app.AppAction
import com.readysetmove.personalworkouts.app.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AppStoreTest {

    @Test
    fun `the store can be setup and trigger workout start`() = runTest {
        val stores = TestStores(testScheduler)
        stores.useAppStore {
            dispatch { AppAction.SetUser(User(
                displayName = "Bob",
                id = "Bobbert",
            )) }
        }
    }
}