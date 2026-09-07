package com.amrc.bleconnect
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: BleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BleAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}
@Composable
fun BleAppScreen(viewModel: BleViewModel) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val receivedData by viewModel.receivedData.collectAsStateWithLifecycle()

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.startScanning()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ESP 32 Polling Monitor",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Status: ${
                        when (connectionState) {
                            is ConnectionState.Idle -> "Idle"
                            is ConnectionState.Scanning -> "Scanning..."
                            is ConnectionState.Connecting -> "Connecting..."
                            is ConnectionState.Connected -> "Connected (Polling every 1s)"
                            is ConnectionState.Error -> (connectionState as ConnectionState.Error).message
                        }
                    }",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Latest Read Value:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = receivedData.ifEmpty { "Waiting for first read..." },
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { permissionLauncher.launch(permissionsToRequest) },
                enabled = connectionState == ConnectionState.Idle
            ) {
                Text("Scan & Start")
            }

            OutlinedButton(
                onClick = { viewModel.disconnect() },
                enabled = connectionState != ConnectionState.Idle
            ) {
                Text("Disconnect")
            }
        }
    }
}