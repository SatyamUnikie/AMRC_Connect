package com.amrc.bleconnect

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

class NfcBleRepository(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = ContextCompat.getSystemService(context, BluetoothManager::class.java)
        manager?.adapter
    }

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(context) }
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val targetServiceUuid = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")

    fun enableNfcReaderMode(activity: Activity) {
        val options = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        nfcAdapter?.enableReaderMode(activity, { tag ->
            handleNfcTag(tag)
        }, options, null)
    }

    fun disableNfcReaderMode(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    private fun handleNfcTag(tag: Tag) {
        val ndef = Ndef.get(tag) ?: return
        try {
            ndef.connect()
            val message = ndef.ndefMessage ?: return
            for (record in message.records) {
                val payload = String(record.payload, StandardCharsets.UTF_8)
                if (payload.contains("MAC=")) {
                    val macAddress = payload.substringAfter("MAC=").substringBefore(";")
                    connectToBleDevice(macAddress)
                    break
                }
            }
        } catch (e: Exception) {
            _connectionState.value = BleConnectionState.Error("Failed to read NFC tag: ${e.localizedMessage}")
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToBleDevice(macAddress: String) {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            _connectionState.value = BleConnectionState.Error("Bluetooth disabled or unsupported")
            return
        }

        _connectionState.value = BleConnectionState.Connecting

        val device: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(macAddress)
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.Disconnected
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = BleConnectionState.Connected(
                    deviceName = gatt.device.name ?: "XIAO BLE Device",
                    macAddress = gatt.device.address
                )
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = BleConnectionState.Disconnected
                gatt.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(targetServiceUuid)
                // Perform read/write/notifications setup here
            }
        }
    }
}