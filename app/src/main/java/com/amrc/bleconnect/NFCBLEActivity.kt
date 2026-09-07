package com.amrc.bleconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class NFCBLEActivity : ComponentActivity() {

    private val viewModel: NfcBleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NfcBleScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause(this)
    }
}

@Composable
fun NfcBleScreen(viewModel: NfcBleViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = connectionState) {
            is BleConnectionState.Disconnected -> {
                Text(text = "Tap an NFC tag to connect to BLE", style = MaterialTheme.typography.bodyLarge)
            }
            is BleConnectionState.Connecting -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Connecting to device...", style = MaterialTheme.typography.bodyLarge)
            }
            is BleConnectionState.Connected -> {
                Text(text = "Connected to ${state.deviceName}", style = MaterialTheme.typography.titleMedium)
                Text(text = "MAC: ${state.macAddress}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.disconnectDevice() }) {
                    Text("Disconnect")
                }
            }
            is BleConnectionState.Error -> {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}