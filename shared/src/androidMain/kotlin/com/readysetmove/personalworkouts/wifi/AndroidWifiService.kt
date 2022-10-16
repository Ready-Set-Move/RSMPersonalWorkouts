package com.readysetmove.personalworkouts.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import io.github.aakira.napier.Napier
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext

private const val classLogTag = "AndroidWifiService"

class AndroidWifiService(
    private val context: Context,
    private val ioDispatcher: CoroutineContext,
) : WifiService {
    private val wifiManager by lazy {
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private var sharedFlow: SharedFlow<WifiService.WifiDeviceActions>? = null

    fun getWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    override fun connectToDevice(
        deviceName: String,
        externalScope: CoroutineScope,
    ): Flow<WifiService.WifiDeviceActions> {
        val methodTag = "${classLogTag}.startConnectCallback"
        Napier.d(tag = methodTag) { "Looking for running connect process" }
        // did we already start the flow? then return it for subscribers
        var zeeFlow = sharedFlow
        if (zeeFlow != null) return zeeFlow

        Napier.d(tag = methodTag) { "Starting connect process" }
        //... otherwise start it to scan, connect and launch the callback to listen to WiFi changes
        zeeFlow = startConnectCallback(deviceName).shareIn(externalScope,
            SharingStarted.WhileSubscribed())
        sharedFlow = zeeFlow
        return zeeFlow
    }

    private fun startConnectCallback(deviceName: String): Flow<WifiService.WifiDeviceActions> {
        val methodTag = "$classLogTag.startConnectCallback"
        return callbackFlow {
            Napier.d(tag = methodTag) { "Starting callback flow" }
            if (!getWifiEnabled()) {
                trySendBlocking(WifiService.WifiDeviceActions.DisConnected(WifiService.WifiExceptions.WifiDisabledException("Wifi needs to be enabled to start scanning.")))
                close()
                return@callbackFlow
            }

            val selectorManager = SelectorManager(ioDispatcher)
            var tcpSocket: Socket? = null
            var udpSocket: ConnectedDatagramSocket? = null

            fun connectUDPSocket() {
                launch(ioDispatcher) {
                    val port = 15353
                    Napier.d(tag = methodTag) { "Launching UDP connector connecting to port $port." }
                    try {
                        val newSocket =
                            withContext(Dispatchers.IO) {
                                DatagramSocket(15353)
                            }
                        Napier.d(tag = methodTag) { "UDP socket connected. Waiting to receive weight updates." }
                        newSocket.broadcast = true
                        Napier.d(tag = methodTag) { "Connected: ${newSocket.isConnected}" }
                        val buffer = ByteArray(2048)
                        val packet = DatagramPacket(buffer, buffer.size)
                        Napier.d(tag = methodTag) { "Waiting to receive UDP answers" }
                        while (true) {
                            withContext(Dispatchers.IO) {
                                newSocket.receive(packet)
                            }
                            Napier.d(tag = methodTag) { "UDP: ${
                                ByteBuffer.wrap(packet.data).order(ByteOrder.LITTLE_ENDIAN)
                                    .float
                            }" }
                        }
                        Napier.d(tag = methodTag) { "Stopped collecting" }
                    } catch (cause: Throwable) {
                        Napier.d(tag = methodTag) { "Connecting to UDP socket failed: $cause" }
                        trySendBlocking(
                            WifiService.WifiDeviceActions.DisConnected(
                                WifiService.WifiExceptions.TCPConnectionFailed(cause.toString())
                            )
                        )
                    }
                }
            }

            fun connectTCPSocket(host: String) {
                launch(ioDispatcher) {
                    Napier.d(tag = methodTag) { "Launching TCP connector." }

                    try {
                        val newSocket = aSocket(selectorManager).tcp().connect(host, 3333)
                        Napier.d(tag = methodTag) { "TCP socket connected" }
                        val receiveChannel = newSocket.openReadChannel()
                        launch {
                            while(true) {
                                Napier.d(tag = methodTag) { "Waiting to receive TCP answers" }
                                receiveChannel.awaitContent()
                                Napier.d(tag = methodTag) { "TCP answers received" }
                                try {
                                    receiveChannel.read {
                                        Napier.d(tag = methodTag) { "Bytes: ${StandardCharsets.UTF_8.decode(it)}" }
                                    }
                                } catch (cause: Throwable) {
                                    Napier.d(tag = methodTag) { cause.toString() }
                                }
                            }
                        }

                        val sendChannel = newSocket.openWriteChannel(autoFlush = true)
                        tcpSocket = newSocket
                        sendChannel.writeFully(byteArrayOf(com.readysetmove.personalworkouts.bluetooth.calibrate,
                            16.toByte(),
                            (1 shr 8).toByte(),
                            (1 shr 16).toByte(),
                            (1 shr 24).toByte()
                        ))
                        sendChannel.flush()
                        Napier.d(tag = methodTag) { "Sent send all" }
                        trySendBlocking(WifiService.WifiDeviceActions.Connected(deviceName))
                    } catch (cause: Throwable) {
                        Napier.d(tag = methodTag) { "Connecting to socket failed: $cause" }
                        trySendBlocking(
                            WifiService.WifiDeviceActions.DisConnected(
                                WifiService.WifiExceptions.TCPConnectionFailed(cause.toString())
                            )
                        )
                    }
                }
            }

            fun connectWifi(deviceName: String) {
                launch(ioDispatcher) {
                    connectivityManager
                        .activeNetwork
                        ?.resolveDNSOr(deviceName) {
                            trySendBlocking(
                                WifiService.WifiDeviceActions.DisConnected(
                                    WifiService.WifiExceptions.TCPConnectionFailed(it)
                                )
                            )
                        }?.let { host ->
                            connectUDPSocket()
                            connectTCPSocket(host)
                        }
                }
            }

            fun connectToAPNetwork() {
                val networkSpecifier = WifiNetworkSpecifier.Builder().setSsid("Ready Set Move Test1").build()
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(networkSpecifier)
                    .build()
                Napier.d(tag = methodTag) { "Requesting to connect" }
                connectivityManager.requestNetwork(request, object : NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Napier.d(tag = methodTag) { "Connected to WiFI AP" }
                        connectUDPSocket()
                        connectTCPSocket("192.168.4.1")
                    }
                })
            }

            connectToAPNetwork()
//            connectWifi(deviceName)

            awaitClose {
                tcpSocket?.close()
                udpSocket?.close()
                selectorManager.close()
                Napier.d(tag = "$methodTag.connectionFlow") { "Flow closed" }
            }
        }
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

    override fun setTara() {
        TODO("Not yet implemented")
    }

    override fun calibrate() {
        TODO("Not yet implemented")
    }

    override fun readSettings() {
        TODO("Not yet implemented")
    }
}