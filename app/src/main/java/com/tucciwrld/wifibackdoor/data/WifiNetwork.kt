package com.tucciwrld.wifibackdoor.data

import java.util.Locale

enum class WifiBand(val label: String) {
    BAND_2_4_GHZ("2.4 GHz"),
    BAND_5_GHZ("5 GHz"),
    BAND_6_GHZ("6 GHz"),
    UNKNOWN("Unknown band");

    companion object {
        fun fromFrequency(frequencyMhz: Int): WifiBand = when {
            frequencyMhz in 2400..2500 -> BAND_2_4_GHZ
            frequencyMhz in 4900..5895 -> BAND_5_GHZ
            frequencyMhz in 5925..7125 -> BAND_6_GHZ
            else -> UNKNOWN
        }
    }
}

enum class SecurityType(val label: String) {
    OPEN("Open"),
    WEP("WEP"),
    WPA("WPA"),
    WPA2("WPA2"),
    WPA3("WPA3"),
    UNKNOWN("Unknown");

    companion object {
        fun fromCapabilities(capabilities: String): SecurityType {
            val caps = capabilities.uppercase(Locale.US)
            return when {
                caps.contains("WPA3") -> WPA3
                caps.contains("RSN") || caps.contains("WPA2") -> WPA2
                caps.contains("WPA") -> WPA
                caps.contains("WEP") -> WEP
                caps.isBlank() -> UNKNOWN
                else -> OPEN
            }
        }
    }
}

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val capabilities: String,
    val hidden: Boolean = false,
) {
    val security: SecurityType
        get() = SecurityType.fromCapabilities(capabilities)

    val enterprise: Boolean
        get() = capabilities.uppercase(Locale.US).contains("EAP")

    val securityLabel: String
        get() = if (enterprise) "${security.label} Enterprise" else security.label

    val band: WifiBand
        get() = WifiBand.fromFrequency(frequencyMhz)

    val channel: Int
        get() = channelForFrequency(frequencyMhz)

    /** Signal strength on a 0..4 scale, matching the familiar Wi-Fi bars. */
    val signalBars: Int
        get() = when {
            rssi >= -55 -> 4
            rssi >= -67 -> 3
            rssi >= -78 -> 2
            rssi >= -88 -> 1
            else -> 0
        }

    val signalLabel: String
        get() = "$rssi dBm"

    companion object {
        fun channelForFrequency(frequencyMhz: Int): Int = when {
            frequencyMhz == 2484 -> 14
            frequencyMhz in 2412..2472 -> (frequencyMhz - 2407) / 5
            frequencyMhz in 5000..5895 -> (frequencyMhz - 5000) / 5
            frequencyMhz in 5955..7115 -> (frequencyMhz - 5950) / 5
            else -> 0
        }
    }
}
