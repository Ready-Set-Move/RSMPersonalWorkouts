package com.readysetmove.personalworkouts.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import com.readysetmove.personalworkouts.wifi.WifiService.WifiNetworkActions.Connected
import com.readysetmove.personalworkouts.wifi.WifiService.WifiNetworkActions.DisConnected
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val classLogTag = "WifiConnection"

class WifiConnection(context: Context, private val wifiConnectionType: WifiService.WifiConnectionType):
    CoroutineScope by CoroutineScope(Dispatchers.IO)
{
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private fun connectExternalWLAN(externalWLANConnection: WifiService.WifiConnectionType.ConnectToExternalWLAN): Flow<WifiService.WifiNetworkActions> {
        return callbackFlow {
            val methodTag = "${classLogTag}.connectExternalWLAN"
            val connectJob = launch(Dispatchers.IO) {
                connectivityManager
                    .activeNetwork
                    ?.resolveDNSOr(externalWLANConnection.deviceDnsName) { cause ->
                        disconnect(WifiService.WifiNetworkExceptions.ResolvingDNSNameFailed(cause))
                    }?.let { host ->
                        Napier.d(tag = methodTag) { "Connected to external WLAN $host" }
                        trySendBlocking(
                            Connected(resolvedHost = host)
                        )
                    }
            }
            awaitClose {
                connectJob.cancel()
            }
        }
    }

    private fun directConnectToAP(directConnection: WifiService.WifiConnectionType.DirectConnection): Flow<WifiService.WifiNetworkActions> {
        return callbackFlow {
            val methodTag = "${classLogTag}.directConnectToAP"
            val networkSpecifier = WifiNetworkSpecifier.Builder().setSsid(directConnection.ssid)
            if (directConnection.passphrase != null) {
                networkSpecifier.setWpa2Passphrase(directConnection.passphrase)
            }
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(networkSpecifier.build())
                .build()
            Napier.d(tag = methodTag) { "Requesting to connect" }
            val networkRequestCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Napier.d(tag = methodTag) { "Connected to WifI AP" }
                    trySendBlocking(
                        Connected(resolvedHost = directAPHostIP)
                    )
                }

                override fun onUnavailable() {
                    Napier.d(tag = methodTag) { "Connection to WifI AP failed" }
                    disconnect(WifiService.WifiNetworkExceptions.DirectAPUnavailable)
                }

                override fun onLost(network: Network) {
                    Napier.d(tag = methodTag) { "Connection to Wifi AP lost" }
                    disconnect(WifiService.WifiNetworkExceptions.ConnectionLost)
                }
            }
            connectivityManager.requestNetwork(request, networkRequestCallback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(networkRequestCallback)
            }
        }
    }

    fun create(): Flow<WifiService.WifiNetworkActions> {
        return when(wifiConnectionType) {
            is WifiService.WifiConnectionType.DirectConnection -> directConnectToAP(wifiConnectionType)
            is WifiService.WifiConnectionType.ConnectToExternalWLAN -> connectExternalWLAN(wifiConnectionType)
        }
    }
}

private fun ProducerScope<WifiService.WifiNetworkActions>.disconnect(
    cause: WifiService.WifiNetworkExceptions,
) {
    trySendBlocking(DisConnected(cause))
    close()
}

private fun Network.resolveDNSOr(deviceName: String, rejected: (cause: String) -> Unit): String? {
    val methodTag = "$classLogTag.resolveDNSOr"
    try {
        val address = getByName(deviceName)
        Napier.d(tag = methodTag) { "Resolved $deviceName to ${address.hostAddress}" }
        return address.hostAddress ?: throw Exception("hostAddress was null")
    } catch (cause: Throwable) {
        val message = "Resolving DNS host failed: $cause"
        Napier.d(tag = methodTag) { message }
        rejected(message)
    }
    return null
}