package com.readysetmove.personalworkouts

import com.readysetmove.personalworkouts.bluetooth.BluetoothAction
import com.readysetmove.personalworkouts.bluetooth.BluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothState

class BluetoothStoreTester(
    tester: StoreTester<BluetoothState, BluetoothAction>,
    val deviceName: String,
    val serviceMock: BluetoothService,
    val initialState: BluetoothState,
)
    : IsStoreTester<BluetoothState, BluetoothAction> by tester