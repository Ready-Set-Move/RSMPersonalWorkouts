package com.readysetmove.personalworkouts.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import com.readysetmove.personalworkouts.device.ConnectionConfiguration
import com.readysetmove.personalworkouts.device.DeviceChange
import com.readysetmove.personalworkouts.device.IsDisconnectCause
import com.readysetmove.personalworkouts.device.WifiConfiguration
import io.github.aakira.napier.Napier
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val classLogTag = "WifiConnection"

class WifiConnection(
    context: Context,
    val connectionConfiguration: ConnectionConfiguration.WifiConnection,
):
    CoroutineScope by CoroutineScope(Dispatchers.IO)
{
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private var currentConnectJob: ProducerScope<DeviceChange>? = null

    fun create(): Flow<DeviceChange> {
        close()
        return connectionConfiguration.configuration.let {
            when (it) {
                is WifiConfiguration.WifiDirectAPConnection ->
                    directConnectToAP(directConnection = it)
                is WifiConfiguration.WifiExternalWLANConnection ->
                    connectExternalWLAN(externalWLANConnection = it)
            }
        }
    }

    fun close() {
        currentConnectJob?.close()
        currentConnectJob = null
    }

    private fun connectExternalWLAN(
        externalWLANConnection: WifiConfiguration.WifiExternalWLANConnection,
    ): Flow<DeviceChange> {
        return callbackFlow {
            currentConnectJob = this
            val methodTag = "${classLogTag}.connectExternalWLAN"

            var closeSockets: (() -> Unit)? = null
            val connectJob = launch(Dispatchers.IO) {
                Napier.d(tag = "$methodTag.connectJob") { "Opening wifi connection" }
                connectivityManager
                    .activeNetwork
                    ?.resolveDNSOr(externalWLANConnection.deviceDnsName) { cause ->
                        disconnect(WifiService.WifiNetworkExceptions.ResolvingDNSNameFailed(cause))
                    }?.let { host ->
                        Napier.d(tag = methodTag) { "Connected to external WLAN $host" }
                        closeSockets = connectSockets(
                            connectionConfiguration = connectionConfiguration,
                            host = host
                        )
                    }
            }
            awaitClose {
                Napier.d(tag = "$methodTag.awaitClose") { "Wifi callback flow closed" }
                closeSockets?.invoke()
                connectJob.cancel()
            }
        }
    }

    private fun directConnectToAP(
        directConnection: WifiConfiguration.WifiDirectAPConnection
    ): Flow<DeviceChange> {
        return callbackFlow {
            currentConnectJob = this
            val methodTag = "${classLogTag}.directConnectToAP"
            val networkSpecifier = WifiNetworkSpecifier.Builder().setSsid(directConnection.ssid)
            // TODO: make this work
            if (directConnection.passphrase != null) {
                networkSpecifier.setWpa2Passphrase(directConnection.passphrase)
            }
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(networkSpecifier.build())
                .build()
            Napier.d(tag = methodTag) { "Requesting to connect to ${directConnection.ssid}" }
            var connectJob: Job? = null
            val networkRequestCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Napier.d(tag = methodTag) { "Connected to WifI AP" }
                    var closeSockets: (() -> Unit)? = null
                    connectJob = launch(Dispatchers.IO) {
                        closeSockets = connectSockets(
                            connectionConfiguration = connectionConfiguration,
                            host = directAPHostIP,
                        )
                    }.also {
                        it.invokeOnCompletion {
                            Napier.d(tag = methodTag) { "Socket connect job completed, closing sockets" }
                            closeSockets?.invoke()
                        }
                    }
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
                Napier.d(tag = methodTag) { "Wifi connection flow closed" }
                connectivityManager.unregisterNetworkCallback(networkRequestCallback)
                connectJob?.cancel()
            }
        }
    }
}

private suspend fun ProducerScope<DeviceChange>.connectSockets(
    connectionConfiguration: ConnectionConfiguration.WifiConnection,
    host: String,
): () -> Unit {
    val methodTag = "$classLogTag.connectSockets"
    var closeUDP: (() -> Unit)? = null
    var closeTCP: (() -> Unit)? = null
    try {
        Napier.d(tag = methodTag) { "Start connecting to TCP socket..," }
        closeTCP = connectTCP(
            port = connectionConfiguration.configuration.tcpPort,
            host = host
        ) {
            trySendBlocking(
                DeviceChange.Connected(connectionConfiguration = connectionConfiguration)
            )
            try {
                Napier.d(tag = methodTag) { "Start connecting to UDP socket..," }
                closeUDP = connectUDP(
                    port = connectionConfiguration.configuration.udpPort,
                )
            } catch (cause: Throwable) {
                Napier.d(tag = methodTag) { "Connecting to UDP socket failed: $cause" }
                closeTCP?.invoke()
                disconnect(WifiService.WifiExceptions.UDPConnectionFailed(cause.toString()))
            }
        }
    } catch (cause: Throwable) {
        Napier.d(tag = methodTag) { "Connecting to TCP socket failed: $cause" }
        disconnect(WifiService.WifiExceptions.TCPConnectionFailed(cause.toString()))
    }
    return {
        Napier.d(tag = methodTag) { "Closing sockets" }
        closeTCP?.apply {
            Napier.d(tag = methodTag) { "Closing TCP socket" }
            invoke()
        }
        closeUDP?.apply {
            Napier.d(tag = methodTag) { "Closing UDP socket" }
            invoke()
        }
    }
}

private fun ProducerScope<DeviceChange>.connectUDP(
    port: Int,
): () -> Unit {
    val methodTag = "$classLogTag.connectUDP"
    Napier.d(tag = methodTag) { "Launching UDP connector connecting to port $port." }

    val udpSocket = DatagramSocket(port)
    Napier.d(tag = methodTag) { "UDP socket bound." }
    udpSocket.broadcast = true
    val buffer = ByteArray(2048)
    val packet = DatagramPacket(buffer, buffer.size)
    launch(Dispatchers.IO) {
        Napier.d(tag = methodTag) { "Listen to weight via UDP" }
        while (isActive) {
            // TODO: check packet for correct type
            try {
                udpSocket.receive(packet)
            } catch (cause: Throwable) {
                Napier.d(tag = methodTag) { "Canceling weight job" }
                cancel(cause.toString())
            }
            val traction = ByteBuffer.wrap(packet.data).order(ByteOrder.LITTLE_ENDIAN)
                .float
            trySendBlocking(DeviceChange.WeightChanged(traction = traction))
        }
    }
    return {
        Napier.d(tag = methodTag) { "UDP socket closed" }
        udpSocket.close()
    }
}

private suspend fun connectTCP(
    port: Int,
    host: String,
    onConnected: () -> Unit
): () -> Unit {
    val methodTag = "$classLogTag.connectTCP"
    Napier.d(tag = methodTag) { "Launching TCP connector connection to $host:$port." }
    val selectorManager = SelectorManager(Dispatchers.IO)
    Napier.d(tag = methodTag) { "Opening TCP socket" }
    val newTCPSocket = aSocket(selectorManager).tcp().connect(host, port = port)
    Napier.d(tag = methodTag) { "TCP socket connected" }
    onConnected()
    return {
        Napier.d(tag = methodTag) { "TCP socket closed" }
        newTCPSocket.run { close() }
        selectorManager.run { close() }
    }
//        val receiveChannel = newTCPSocket.openReadChannel()
            // TODO: read callbacks and writing
//                                    Napier.d(tag = methodTag) { "Waiting to receive TCP answers" }
//                                    receiveChannel.awaitContent()
//                                    Napier.d(tag = methodTag) { "TCP answers received" }
//                                    try {
//                                        receiveChannel.read {
//                                            Napier.d(tag = methodTag) { "TCP: ${StandardCharsets.UTF_8.decode(it)}" }
//                                        }
//                                    } catch (cause: Throwable) {
//                                        Napier.d(tag = methodTag) { cause.toString() }
//                                    }
}

// TODO: send cmd
//                                val sendChannel = tcpSocket.openWriteChannel(autoFlush = true)
//                                sendChannel.writeFully(ByteBuffer.wrap(byteArrayOf(readAll,
//                                    1.toByte(),
//                                    (1 shr 8).toByte(),
//                                    (1 shr 16).toByte(),
//                                    (1 shr 24).toByte()
//                                )))
//                        sendChannel.writeFully(byteArrayOf(com.readysetmove.personalworkouts.bluetooth.calibrate,
//                            16.toByte(),
//                            (1 shr 8).toByte(),
//                            (1 shr 16).toByte(),
//                            (1 shr 24).toByte()
//                        ))
//                                sendChannel.flush()
//                                Napier.d(tag = methodTag) { "Sent send all" }


private fun ProducerScope<DeviceChange>.disconnect(
    cause: IsDisconnectCause,
) {
    trySendBlocking(DeviceChange.Disconnected(cause))
    cancel()
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