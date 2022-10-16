package com.readysetmove.personalworkouts.wifi

import com.readysetmove.personalworkouts.device.DeviceConfiguration
import com.readysetmove.personalworkouts.device.DeviceService

interface WifiService: DeviceService<WifiService.WifiDeviceActions> {

    sealed class WifiExceptions {
        class WifiDisabledException(message: String): WifiExceptions()
        class TCPConnectionFailed(message: String): WifiExceptions()
    }

    sealed class WifiDeviceActions {
        data class Connected(val deviceName: String) : WifiDeviceActions()
        data class DisConnected(val cause: WifiExceptions) : WifiDeviceActions()
        data class WeightChanged(val traction: Float) : WifiDeviceActions()
        data class DeviceDataChanged(val deviceConfiguration: DeviceConfiguration) : WifiDeviceActions()
    }
}