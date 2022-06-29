package com.readysetmove.personalworkouts.bluetooth

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.github.aakira.napier.Napier
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BTLECallback(
    private val androidContext: Context,
    private val maxReconnectAttempts: Int,
    private val device: BluetoothDevice,
    private val deviceName: String,
    rootLogTag: String,
    private val onDeviceConnected: () -> Unit,
    private val onDisconnect: (cause: BluetoothService.BluetoothException) -> Unit,
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
            enableTractionNotifications(gatt, classLogTag)
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
    ) {
        val logTag = "$classLogTag.onCharacteristicChanged"
        Napier.d(tag = logTag, message = "started")
        if (characteristic == null) return
        val weight =
            ByteBuffer.wrap(characteristic.value).order(ByteOrder.LITTLE_ENDIAN)
                .float
        onWeightReceived(weight)
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int,
    ) {
        val logTag = "$classLogTag.onDescriptorWrite"
        Napier.d(tag = logTag, message = "received ${descriptor?.uuid}")
        if (gatt == null || descriptor?.uuid != cccdUuid) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return reconnect(status = status, gatt = gatt, logTag)
        }
        Napier.d(tag = logTag, message = "CCC descriptor successfully enabled")
        onDeviceConnected()
    }

    private fun enableTractionNotifications(
        gatt: BluetoothGatt,
        logTag: String?,
    ) {
        if (Build.VERSION.SDK_INT >= 31 && androidContext.checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return onDisconnect(BluetoothService.BluetoothException.BluetoothPermissionNotGrantedException(
                "Needed permissions no longer granted. Device: ${device.address}"))
        }
        val tractionCharacteristic =
            gatt.getService(serviceUuid)
                .getCharacteristic(tractionUuid)

        tractionCharacteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (!gatt.setCharacteristicNotification(tractionCharacteristic,
                    true)
            ) {
                return onDisconnect(BluetoothService.BluetoothException.ConnectFailedException("setCharacteristicNotification failed for tractionCharacteristic: $tractionCharacteristic"))
            }

            cccDescriptor.value =
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccDescriptor)
            Napier.d(tag = logTag, message = "traction characteristic enabled")
        }
            ?: onDisconnect(BluetoothService.BluetoothException.ConnectFailedException("Traction characteristic does not contain CCC descriptor. $tractionCharacteristic"))
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