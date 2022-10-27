package com.readysetmove.personalworkouts.device

import com.readysetmove.personalworkouts.state.Action
import com.readysetmove.personalworkouts.state.Effect
import com.readysetmove.personalworkouts.state.State
import com.readysetmove.personalworkouts.state.Store

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

data class DeviceState(
    val connectionConfiguration: ConnectionConfiguration? = null,
    val traction: Float = 0.0f,
    val deviceConfiguration: DeviceConfiguration? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
) : State

sealed class WifiConfiguration {
    data class WifiDirectAPConnection(val ssid: String = "isoX", val passphrase: String?): WifiConfiguration()
    data class WifiExternalWLANConnection(val deviceDnsName: String): WifiConfiguration()
}

sealed class ConnectionConfiguration {
    data class WifiConnection(val configuration: WifiConfiguration): ConnectionConfiguration()
    data class BLEConnection(val deviceName: String): ConnectionConfiguration()
}

interface IsDisconnectCause

sealed class DeviceChange {
    data class WeightChanged(val traction: Float) : DeviceChange()
    data class DeviceDataChanged(val deviceConfiguration: DeviceConfiguration) : DeviceChange()
    data class Connected(val connectionConfiguration: ConnectionConfiguration) : DeviceChange()
    data class Disconnected(val cause: IsDisconnectCause) : DeviceChange()
}

sealed class DeviceAction: Action {
    data class SetConnectionType(val connectionConfiguration: ConnectionConfiguration): DeviceAction()
    object ScanAndConnect: DeviceAction()
    object ResetConnection: DeviceAction()
    object ReadSettings: DeviceAction()
    object SetTara: DeviceAction()
    object Calibrate: DeviceAction()
}

sealed class DeviceSideEffect : Effect {
    object ConnectionNotConfigured : DeviceSideEffect()
    data class Error(val error: Exception) : DeviceSideEffect()
    data class DeviceDisconnected(
        val configuration: DeviceConfiguration?,
        val disconnectCause: IsDisconnectCause?
    ) : DeviceSideEffect()
}

interface IsDeviceStore: Store<DeviceState, DeviceAction, DeviceSideEffect>