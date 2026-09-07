package com.amrc.bleconnect


sealed interface BleConnectionState {
    object Disconnected : BleConnectionState
    object Connecting : BleConnectionState
    data class Connected(val deviceName: String, val macAddress: String) : BleConnectionState
    data class Error(val message: String) : BleConnectionState
}