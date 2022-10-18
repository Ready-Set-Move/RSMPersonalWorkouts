package com.readysetmove.personalworkouts.device

@kotlinx.serialization.Serializable
data class DeviceConfiguration(
    val name: String,
    val namePostfix: String,
    val wlanSSID: String,
    val wlanPassword: String,
    val accessPointPassword: String,
    val dnsName: String,
    val average: Int,
    val gain128: Boolean,
    val scaleFactor: Float,
    val voltage: Float,
    val autoTara: Boolean,
    val temperature: Float,
    val wifiStartMode: Int,
    val wifiState: Int,
)

object DeviceConfigurationBuilder

fun DeviceConfigurationBuilder.default(): DeviceConfiguration {
    return DeviceConfiguration(
        name="name",
        namePostfix="namePostfix",
        wlanSSID="wlanSSID",
        wlanPassword="wlanPassword",
        accessPointPassword="accessPointPassword",
        dnsName="dnsName",
        average=0,
        gain128=true,
        scaleFactor=0.0f,
        voltage=0.0f,
        autoTara=true,
        temperature=0.0f,
        wifiStartMode=0,
        wifiState=0
    )
}