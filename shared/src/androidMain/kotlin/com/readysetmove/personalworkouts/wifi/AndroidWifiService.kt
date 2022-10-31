package com.readysetmove.personalworkouts.wifi

import android.content.Context
import android.net.wifi.WifiManager
import com.readysetmove.personalworkouts.device.ConnectionConfiguration
import com.readysetmove.personalworkouts.device.DeviceChange
import com.readysetmove.personalworkouts.wifi.WifiService.WifiExceptions.WifiDisabledException
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val classLogTag = "AndroidWifiService"

class AndroidWifiService(
    private val context: Context,
) : WifiService, CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val wifiManager by lazy {
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    val wifiEnabled: Boolean = wifiManager.isWifiEnabled

    private var wifiConnection: WifiConnection? = null

    override fun connectToDevice(
        wifiConfiguration: ConnectionConfiguration.WifiConnection,
    ): Flow<DeviceChange> {
        val methodTag = "${classLogTag}.connectToDevice"
        Napier.d(tag = methodTag) { "Looking for running connect process" }
        // did we already start the flow? then return it for subscribers

        if (!wifiEnabled)
            return flow {
                emit(DeviceChange.Disconnected(WifiDisabledException("Wifi needs to be enabled to start scanning.")))
            }

        wifiConnection?.close()
        Napier.d(tag = methodTag) { "Starting connect process" }
        //... otherwise start it to scan, connect and launch the callback to listen to WiFi changes
        return WifiConnection(
            context = context,
            connectionConfiguration = wifiConfiguration
        )
            .also {
                wifiConnection = it
            }
            .create()
    }

    override fun setTara() {
        TODO("Not yet implemented")
    }

    override fun calibrate() {
        TODO("Not yet implemented")
    }

    override fun readSettings() {
//        connectTCPSocket()
    }

    override fun disconnect() {
        Napier.d(tag = classLogTag) { "disconnect" }
        wifiConnection?.close()
    }
}