package com.readysetmove.personalworkouts.bluetooth

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.readysetmove.personalworkouts.device.DeviceConfiguration
import io.github.aakira.napier.Napier
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.*

private val json = Json { ignoreUnknownKeys = true }

class BTLECallback(
    private val androidContext: Context,
    private val maxReconnectAttempts: Int,
    private val device: BluetoothDevice,
    private val deviceName: String,
    rootLogTag: String,
    private val onDeviceConnected: () -> Unit,
    private val onDisconnect: (cause: BluetoothService.BluetoothException) -> Unit,
    private val onDeviceDataReceived: (deviceConfiguration: DeviceConfiguration) -> Unit,
    private val onWeightReceived: (weight: Float) -> Unit,
) : BluetoothGattCallback() {
    private var reconnectAttemptsLeft = maxReconnectAttempts
    private val classLogTag = "$rootLogTag.BTLECallback"

    override fun onConnectionStateChange(
        gatt: BluetoothGatt?,
        status: Int,
        newState: Int,
    ) {
        with(device) {
            val logTag = "$classLogTag.onConnectionStateChange"
            Napier.d(tag = logTag, message = "state changed: status=$status newState=$newState")
            if (gatt == null) return
            if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return onDisconnect(BluetoothService.BluetoothException.BluetoothPermissionNotGrantedException(
                    "Needed permissions no longer granted. Device: ${device.address}"))
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return reconnect(status = status, gatt = gatt, logTag)
            }
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                reconnectAttemptsLeft = maxReconnectAttempts
                Napier.d(tag = logTag, message = "device connected: $deviceName@$address")
                gatt.discoverServices()
            }
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Napier.d(tag = logTag, message = "device disconnected: $deviceName@$address")
                onDisconnect(BluetoothService.BluetoothException.ConnectFailedException("Device disconnected."))
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        val logTag = "$classLogTag.onServicesDiscovered"
        if (gatt == null) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            reconnect(status = status, gatt = gatt, logTag)
        } else {
            Napier.d("services discovered", tag = logTag)
            if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return onDisconnect(BluetoothService.BluetoothException.BluetoothPermissionNotGrantedException(
                    "Needed permissions no longer granted. Device: ${device.address}"))
            }
            gatt.requestMtu(517)
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (gatt == null || status != BluetoothGatt.GATT_SUCCESS) {
            Napier.d(tag = classLogTag) { "Failed to change MTU" }
            return
        }
        Napier.d(tag = classLogTag) { "MTU changed to $mtu" }
        enableNotifications4Characteristic(gatt = gatt, logTag = classLogTag, characteristicUUID = tractionUuid)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
    ) {
        val logTag = "$classLogTag.onCharacteristicChanged"
        if (characteristic == null) {
            Napier.d(tag = logTag) { "Null characteristic received" }
            return
        }

        when (characteristic.uuid) {
            tractionUuid -> {
                val weight =
                    ByteBuffer.wrap(characteristic.value).order(ByteOrder.LITTLE_ENDIAN)
                        .float
                onWeightReceived(weight)
            }
            dataUuid -> {
                Napier.d(tag = logTag) { "Data received" }
                val payloadString = String(characteristic.value, StandardCharsets.UTF_8)
                Napier.d(tag = logTag) { "BLE Payload: $payloadString" }
                // TODO: serialize correct types and updates:
                // Full config: {"DevName":"Ready Set Move","PrivName":"Test1","StaSsid":"joes WLAN","StaPw":"7Fghyz49yym17QpLas","ApPw":"MyPassword","DnsName":"rsm","Average":2,"Gain128":true,"Scale":"inf","Volt":"5.11","AutoTara":true,"Tempr":"22.62","WiFiStartMode":4,"WiFiState":1}
                // WPS response: {"Wps":"Success","StaPw":"cotty-faber-acrobat-swell-drawl-mazes-marlin-phosphate","StaSsid":"Ready Set Move Personal Training"}
                // Partial updates like: {"WiFiState":2}
                val deviceConfiguration = json.decodeFromString<DeviceConfiguration>(payloadString)
                onDeviceDataReceived(deviceConfiguration)
            }
            else -> {
                Napier.d(tag = logTag) { "Unknown characteristic received: ${characteristic.uuid} with ${characteristic.value}" }
            }
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int,
    ) {
        val logTag = "$classLogTag.onDescriptorWrite"
        Napier.d(tag = logTag, message = "received ${descriptor?.uuid}")
        if (gatt == null) return
        descriptor?.let {
            if (it.uuid != cccdUuid) return

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Napier.d(tag = logTag, message = "Descriptor write not successful for ${it.characteristic.uuid}. Attempt reconnect.")
                return reconnect(status = status, gatt = gatt, logTag)
            }
            Napier.d(tag = logTag, message = "CCC descriptor successfully enabled for ${it.characteristic.uuid}.")

            if (it.characteristic.uuid == tractionUuid) {
                Napier.d(tag = logTag, message = "Traction notification descriptor enabled. Attempt connecting data descriptor.")
                enableNotifications4Characteristic(gatt = gatt, logTag = classLogTag, characteristicUUID = dataUuid)
            } else {
                Napier.d(tag = logTag, message = "All notification descriptors enabled. Device connected")
                onDeviceConnected()
            }
        }
    }

    private fun enableNotifications4Characteristic(
        gatt: BluetoothGatt,
        logTag: String?,
        characteristicUUID: UUID,
    ) {
        if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return onDisconnect(BluetoothService.BluetoothException.BluetoothPermissionNotGrantedException(
                "Needed permissions no longer granted. Device: ${device.address}"))
        }
        val characteristic =
            gatt.getService(serviceUuid)
                .getCharacteristic(characteristicUUID)

        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (!gatt.setCharacteristicNotification(characteristic,
                    true)
            ) {
                return onDisconnect(BluetoothService.BluetoothException.ConnectFailedException("setCharacteristicNotification failed for characteristic: $characteristic"))
            }

            cccDescriptor.value =
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccDescriptor)
            Napier.d(tag = logTag, message = "characteristic $characteristicUUID enabled")
        }
            ?: onDisconnect(BluetoothService.BluetoothException.ConnectFailedException("characteristic $characteristicUUID does not contain CCC descriptor. $characteristic"))
    }

    private fun reconnect(status: Int, gatt: BluetoothGatt, logTag: String) {
        with(device) {
            val reconnectMethodTag = "reconnect"
            Log.d("$logTag.$reconnectMethodTag", "status was failed: $status.")
            if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return onDisconnect(BluetoothService.BluetoothException.BluetoothPermissionNotGrantedException(
                    "Needed permissions no longer granted. Device: ${device.address}"))
            }
            gatt.close()

            if (status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION || status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                Log.d("$logTag.$reconnectMethodTag",
                    "status failed with insufficient encryption or authentication. Bonding is not supported yet")
                // for now we don't support bonding if we want to refer to: https://punchthrough.com/android-ble-guide/
            }

            if (reconnectAttemptsLeft == 0) {
                Log.d("$logTag.$reconnectMethodTag",
                    "Reconnect failed to many times. Stop retry.")
                return onDisconnect(BluetoothService.BluetoothException.ConnectFailedException("Too many gatt connection attempts failed. Device: $address"))
            }
            // TODO: send action to notify user
            Log.d("$logTag.$classLogTag", "Trying to reconnect.")
            reconnectAttemptsLeft--
            connectGatt(androidContext,
                false,
                this@BTLECallback,
                BluetoothDevice.TRANSPORT_LE)
        }
    }
}