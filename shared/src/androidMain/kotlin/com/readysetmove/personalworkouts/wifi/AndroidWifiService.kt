package com.readysetmove.personalworkouts.wifi

import android.content.Context
import android.net.wifi.WifiManager
import com.readysetmove.personalworkouts.device.ConnectionConfiguration
import com.readysetmove.personalworkouts.device.DeviceChange
import com.readysetmove.personalworkouts.device.IsDisconnectCause
import com.readysetmove.personalworkouts.wifi.WifiService.WifiExceptions.*
import com.readysetmove.personalworkouts.wifi.WifiService.WifiNetworkActions
import com.readysetmove.personalworkouts.wifi.WifiService.WifiNetworkExceptions
import io.github.aakira.napier.Napier
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val classLogTag = "AndroidWifiService"

class AndroidWifiService(
    private val context: Context,
) : WifiService, CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val wifiManager by lazy {
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    private var sharedFlow: SharedFlow<DeviceChange>? = null

    fun getWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    override fun connectToDevice(
        wifiConfiguration: ConnectionConfiguration.WifiConnection,
        externalScope: CoroutineScope
    ): Flow<DeviceChange> {
        val methodTag = "${classLogTag}.startConnectCallback"
        Napier.d(tag = methodTag) { "Looking for running connect process" }
        // did we already start the flow? then return it for subscribers
        var zeeFlow = sharedFlow
        if (zeeFlow != null) return zeeFlow

        Napier.d(tag = methodTag) { "Starting connect process" }
        //... otherwise start it to scan, connect and launch the callback to listen to WiFi changes
        zeeFlow = startConnectCallback(wifiConfiguration).shareIn(externalScope,
            SharingStarted.WhileSubscribed())
        sharedFlow = zeeFlow
        return zeeFlow
    }

    private fun startConnectCallback(wifiConfiguration: ConnectionConfiguration.WifiConnection): Flow<DeviceChange> {
        val methodTag = "$classLogTag.startConnectCallback"
        return callbackFlow {
            Napier.d(tag = methodTag) { "Starting callback flow" }
            if (!getWifiEnabled())
                return@callbackFlow disconnect(WifiDisabledException("Wifi needs to be enabled to start scanning."))

            val selectorManager = SelectorManager(Dispatchers.IO)
            var udpSocket: DatagramSocket? = null

            WifiConnection(
                context = context,
                wifiConfiguration = wifiConfiguration.configuration
            ).create().collect {
                when(it) {
                    is WifiNetworkActions.DisConnected -> {
                        disconnect(when(it.cause) {
                            is WifiNetworkExceptions.ConnectionLost ->
                                NetworkDisconnected
                            is WifiNetworkExceptions.ResolvingDNSNameFailed ->
                                ConnectingToNetworkFailed(it.cause.message)
                            is WifiNetworkExceptions.DirectAPUnavailable ->
                                ConnectingToDeviceAPFailed
                        })
                    }
                    is WifiNetworkActions.Connected -> {
                        val port = 15353
                        Napier.d(tag = methodTag) { "Launching UDP connector connecting to port $port." }
                        try {
                            val newSocket =
                                withContext(Dispatchers.IO) {
                                    DatagramSocket(15353)
                                }
                            udpSocket = newSocket
                            Napier.d(tag = methodTag) { "UDP socket bound." }
                            newSocket.broadcast = true
                            val buffer = ByteArray(2048)
                            val packet = DatagramPacket(buffer, buffer.size)
                            launch {
                                while (true) {
                                    withContext(Dispatchers.IO) {
                                        newSocket.receive(packet)
                                    }
                                    val traction = ByteBuffer.wrap(packet.data).order(ByteOrder.LITTLE_ENDIAN)
                                        .float
                                    trySendBlocking(
                                        DeviceChange.WeightChanged(traction = traction)
                                    )
                                }
                            }



                            Napier.d(tag = methodTag) { "Launching TCP connector." }

                            try {
                                launch(Dispatchers.IO) {
                                    val tcpSocket = aSocket(selectorManager).tcp().connect(it.resolvedHost, 3333)
                                    Napier.d(tag = methodTag) { "TCP socket connected" }
//                                    val receiveChannel = tcpSocket.openReadChannel()
//
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

                                    trySendBlocking(
                                        DeviceChange.Connected(connectionConfiguration = wifiConfiguration)
                                    )
                                }


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
                            } catch (cause: Throwable) {
                                disconnect(TCPConnectionFailed(cause.toString()))
                            }
                        } catch (cause: Throwable) {
                            Napier.d(tag = methodTag) { "Connecting to UDP socket failed: $cause" }
                            disconnect(UDPConnectionFailed(cause.toString()))
                        }

                    }
                }
            }

            awaitClose {
                udpSocket?.close()
                selectorManager.close()
                sharedFlow = null
                Napier.d(tag = "$methodTag.connectionFlow") { "Flow closed" }
            }
        }
    }

    private fun ProducerScope<DeviceChange>.disconnect(
        cause: IsDisconnectCause,
    ) {
        trySendBlocking(DeviceChange.Disconnected(cause))
        close()
        sharedFlow = null
    }

    suspend fun connectTCPSocket(host: String): Flow<DeviceChange> {
        val methodTag = "$classLogTag.connectTCPSocket"
        val selectorManager = SelectorManager(Dispatchers.IO)
        return callbackFlow {
        }
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
}