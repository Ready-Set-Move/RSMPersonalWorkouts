package com.readysetmove.personalworkouts.wifi

import com.readysetmove.personalworkouts.device.DeviceConfiguration
import com.readysetmove.personalworkouts.device.DeviceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

const val directAPHostIP = "192.168.4.1"

interface WifiService: DeviceService {
    fun connectToDevice(
        wifiConnectionType: WifiConnectionType,
        externalScope: CoroutineScope,
    ): Flow<WifiDeviceActions>

    sealed class WifiConnectionType {
        data class DirectConnection(val ssid: String = "isoX", val passphrase: String?): WifiConnectionType()
        data class ConnectToExternalWLAN(val deviceDnsName: String): WifiConnectionType()
    }

    sealed class WifiNetworkExceptions {
        data class ResolvingDNSNameFailed(val message: String): WifiNetworkExceptions()
        object ConnectionLost: WifiNetworkExceptions()
        object DirectAPUnavailable: WifiNetworkExceptions()
    }

    sealed class WifiNetworkActions {
        data class Connected(val resolvedHost: String) : WifiNetworkActions()
        data class DisConnected(val cause: WifiNetworkExceptions) : WifiNetworkActions()
    }

    sealed class WifiExceptions {
        data class WifiDisabledException(val message: String): WifiExceptions()
        data class TCPConnectionFailed(val message: String): WifiExceptions()
        data class UDPConnectionFailed(val message: String): WifiExceptions()
        data class ConnectingToNetworkFailed(val message: String): WifiExceptions()
        object ConnectingToDeviceAPFailed: WifiExceptions()
        object NetworkDisconnected: WifiExceptions()
    }

    sealed class WifiDeviceActions {
        data class Connected(val connectedHost: String) : WifiDeviceActions()
        data class DisConnected(val cause: WifiExceptions) : WifiDeviceActions()
        data class WeightChanged(val traction: Float) : WifiDeviceActions()
        data class DeviceDataChanged(val deviceConfiguration: DeviceConfiguration) : WifiDeviceActions()
    }
}