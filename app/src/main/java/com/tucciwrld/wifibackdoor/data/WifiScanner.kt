package com.tucciwrld.wifibackdoor.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Thin wrapper around [WifiManager] so the rest of the app never touches platform types. */
object WifiScanner {

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasPermissions(context: Context): Boolean = requiredPermissions().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun wifiManager(context: Context): WifiManager? =
        context.applicationContext.getSystemService(WifiManager::class.java)

    fun toNetworks(results: List<ScanResult>): List<WifiNetwork> = results
        .map { result ->
            val ssid = result.SSID?.trim().orEmpty()
            WifiNetwork(
                ssid = ssid.ifEmpty { "(hidden network)" },
                bssid = result.BSSID ?: "unknown",
                rssi = result.level,
                frequencyMhz = result.frequency,
                capabilities = result.capabilities.orEmpty(),
                hidden = ssid.isEmpty(),
            )
        }
        .sortedByDescending { it.rssi }
}
