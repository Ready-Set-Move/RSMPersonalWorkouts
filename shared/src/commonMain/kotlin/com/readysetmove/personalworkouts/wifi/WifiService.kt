package com.readysetmove.personalworkouts.wifi

import com.readysetmove.personalworkouts.device.ConnectionConfiguration
import com.readysetmove.personalworkouts.device.DeviceChange
import com.readysetmove.personalworkouts.device.DeviceService
import com.readysetmove.personalworkouts.device.IsDisconnectCause
import kotlinx.coroutines.flow.Flow

const val directAPHostIP = "192.168.4.2"

interface WifiService: DeviceService {
    fun connectToDevice(
        wifiConfiguration: ConnectionConfiguration.WifiConnection,
    ): Flow<DeviceChange>

    sealed class WifiNetworkExceptions: IsDisconnectCause {
        data class ResolvingDNSNameFailed(val message: String): WifiNetworkExceptions()
        object ConnectionLost: WifiNetworkExceptions()
        object DirectAPUnavailable: WifiNetworkExceptions()
    }

    sealed class WifiExceptions: IsDisconnectCause {
        data class WifiDisabledException(val message: String): WifiExceptions()
        data class TCPConnectionFailed(val message: String): WifiExceptions()
        data class UDPConnectionFailed(val message: String): WifiExceptions()
    }
}