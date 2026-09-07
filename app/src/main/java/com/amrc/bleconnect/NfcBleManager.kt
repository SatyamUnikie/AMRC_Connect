package com.amrc.bleconnect

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.UUID

class NfcBleManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    private var nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    var bluetoothGatt: BluetoothGatt? = null

    // Replace with target GATT service UUID
    private val targetServiceUuid = java.util.UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

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
                Log.d("NfcBleManager", "NFC Payload Read: $payload")

                // Parse payload string formatted like: "MAC=AA:BB:CC:DD:EE:FF;ID=1234"
                if (payload.contains("MAC=")) {
                    val macAddress = payload.substringAfter("MAC=").substringBefore(";")
                    Log.i("NfcBleManager", "Extracted Target MAC: $macAddress")

                    // Stop NFC reading and connect directly to target MAC
                    connectToBleDevice(macAddress)
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("NfcBleManager", "Error reading NDEF tag", e)
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToBleDevice(macAddress: String) {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e("NfcBleManager", "Bluetooth is disabled or unsupported")
            return
        }

        // Direct connection by MAC Address bypassing active scanning loop
        val device: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(macAddress)

        Log.i("NfcBleManager", "Initiating direct GATT connection to ${device.address}")
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("NfcBleManager", "Successfully connected to BLE Device")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("NfcBleManager", "Disconnected from BLE Device")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("NfcBleManager", "GATT Services Discovered")
                val service = gatt.getService(targetServiceUuid)
                // Ready for characteristic read/write/notifications
            }
        }
    }
}