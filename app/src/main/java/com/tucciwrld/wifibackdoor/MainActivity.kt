package com.tucciwrld.wifibackdoor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tucciwrld.wifibackdoor.data.WifiScanner
import com.tucciwrld.wifibackdoor.ui.WifiScreen
import com.tucciwrld.wifibackdoor.ui.theme.WifiBackDoorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WifiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WifiBackDoorTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) {
                    viewModel.onPermissionsResult()
                }

                WifiScreen(
                    state = state,
                    onScan = viewModel::startScan,
                    onRequestPermissions = {
                        permissionLauncher.launch(WifiScanner.requiredPermissions())
                    },
                )
            }
        }
    }
}
