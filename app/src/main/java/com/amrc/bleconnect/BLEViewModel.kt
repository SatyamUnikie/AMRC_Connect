package com.amrc.bleconnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = BLEManager(application.applicationContext)

    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val receivedData: StateFlow<String> = bleManager.receivedData

    fun startScanning() {
        bleManager.startScan()
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
    }
}