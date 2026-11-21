package com.tucciwrld.wifibackdoor

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.tucciwrld.wifibackdoor.data.Finding
import com.tucciwrld.wifibackdoor.data.RiskAnalyzer
import com.tucciwrld.wifibackdoor.data.WifiNetwork
import com.tucciwrld.wifibackdoor.data.WifiScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WifiUiState(
    val permissionGranted: Boolean = false,
    val wifiEnabled: Boolean = true,
    val scanning: Boolean = false,
    val networks: List<WifiNetwork> = emptyList(),
    val findings: List<Finding> = emptyList(),
    val message: String? = null,
)

class WifiViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val _state = MutableStateFlow(
        WifiUiState(permissionGranted = WifiScanner.hasPermissions(context)),
    )
    val state: StateFlow<WifiUiState> = _state.asStateFlow()

    private var receiver: BroadcastReceiver? = null

    fun onPermissionsResult() {
        val granted = WifiScanner.hasPermissions(context)
        _state.update {
            it.copy(
                permissionGranted = granted,
                message = if (granted) null else "Location and nearby Wi-Fi access are needed to list networks.",
            )
        }
        if (granted) startScan()
    }

    fun startScan() {
        if (!WifiScanner.hasPermissions(context)) {
            _state.update {
                it.copy(
                    permissionGranted = false,
                    scanning = false,
                    message = "Grant Wi-Fi access to scan for nearby networks.",
                )
            }
            return
        }

        val manager = WifiScanner.wifiManager(context)
        if (manager == null) {
            _state.update {
                it.copy(scanning = false, message = "This device does not expose a Wi-Fi manager.")
            }
            return
        }

        val enabled = manager.isWifiEnabled
        if (!enabled) {
            _state.update {
                it.copy(
                    wifiEnabled = false,
                    scanning = false,
                    message = "Wi-Fi is off. Turn Wi-Fi on, then scan again.",
                )
            }
            return
        }

        _state.update { it.copy(wifiEnabled = true, scanning = true, message = null) }

        registerResultsReceiver(manager)
        val started = runCatching { manager.startScan() }.getOrDefault(false)
        publishResults(runCatching { manager.scanResults }.getOrDefault(emptyList()))

        if (!started) {
            _state.update {
                it.copy(message = "The system throttled the scan. Showing the most recent results.")
            }
        }
    }

    private fun registerResultsReceiver(manager: WifiManager) {
        if (receiver != null) return

        val resultsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                publishResults(runCatching { manager.scanResults }.getOrDefault(emptyList()))
            }
        }

        receiver = resultsReceiver
        ContextCompat.registerReceiver(
            context,
            resultsReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun publishResults(results: List<ScanResult>) {
        val networks = WifiScanner.toNetworks(results)
        _state.update {
            it.copy(
                networks = networks,
                findings = RiskAnalyzer.analyze(networks),
                scanning = false,
            )
        }
    }

    override fun onCleared() {
        receiver?.let { registered -> runCatching { context.unregisterReceiver(registered) } }
        receiver = null
        super.onCleared()
    }
}
