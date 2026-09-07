package com.amrc.bleconnect



import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class NfcBleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NfcBleRepository(application.applicationContext)
    val connectionState: StateFlow<BleConnectionState> = repository.connectionState

    fun onResume(activity: Activity) {
        repository.enableNfcReaderMode(activity)
    }

    fun onPause(activity: Activity) {
        repository.disableNfcReaderMode(activity)
    }

    fun disconnectDevice() {
        repository.disconnect()
    }
}