package com.readysetmove.personalworkouts.wifi

import com.readysetmove.personalworkouts.device.ConnectionConfiguration
import com.readysetmove.personalworkouts.device.DeviceChange
import com.readysetmove.personalworkouts.device.DeviceService
import com.readysetmove.personalworkouts.device.IsDisconnectCause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

const val directAPHostIP = "192.168.4.1"

interface WifiService: DeviceService {
    fun connectToDevice(
        wifiConfiguration: ConnectionConfiguration.WifiConnection,
        externalScope: CoroutineScope,
    ): Flow<DeviceChange>

    sealed class WifiNetworkExceptions {
        data class ResolvingDNSNameFailed(val message: String): WifiNetworkExceptions()
        object ConnectionLost: WifiNetworkExceptions()
        object DirectAPUnavailable: WifiNetworkExceptions()
    }

    sealed class WifiNetworkActions {
        data class Connected(val resolvedHost: String) : WifiNetworkActions()
        data class DisConnected(val cause: WifiNetworkExceptions) : WifiNetworkActions()
    }

    sealed class WifiExceptions: IsDisconnectCause {
        data class WifiDisabledException(val message: String): WifiExceptions()
        data class TCPConnectionFailed(val message: String): WifiExceptions()
        data class UDPConnectionFailed(val message: String): WifiExceptions()
        data class ConnectingToNetworkFailed(val message: String): WifiExceptions()
        object ConnectingToDeviceAPFailed: WifiExceptions()
        object NetworkDisconnected: WifiExceptions()
    }
}