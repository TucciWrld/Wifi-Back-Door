package com.tucciwrld.wifibackdoor.data

enum class Severity(val label: String, val rank: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2),
}

enum class FindingKind(val title: String, val severity: Severity, val explanation: String) {
    EVIL_TWIN(
        title = "Possible evil twin",
        severity = Severity.HIGH,
        explanation = "This network name is broadcast by more than one access point and they do not share " +
            "the same security settings. A rogue access point can copy a trusted name to intercept traffic.",
    ),
    OPEN(
        title = "No encryption",
        severity = Severity.MEDIUM,
        explanation = "Anyone in range can read or change the traffic on this network without a password.",
    ),
    WEAK_CRYPTO(
        title = "Outdated encryption",
        severity = Severity.MEDIUM,
        explanation = "WEP and first-generation WPA are broken and can be cracked quickly. Prefer WPA2 or WPA3.",
    ),
    HIDDEN(
        title = "Hidden network",
        severity = Severity.LOW,
        explanation = "A hidden network still broadcasts its presence. Hiding the name adds no real security.",
    ),
}

data class Finding(
    val network: WifiNetwork,
    val kind: FindingKind,
    val detail: String,
)

/**
 * Looks for the patterns a Wi-Fi "back door" leaves behind: a trusted name replayed by a second
 * access point, unencrypted or obsolete security, and networks that hide their name.
 */
object RiskAnalyzer {

    fun analyze(networks: List<WifiNetwork>): List<Finding> {
        val candidates = mutableMapOf<String, MutableList<Finding>>()

        fun add(finding: Finding) {
            candidates.getOrPut(finding.network.bssid) { mutableListOf() }.add(finding)
        }

        networks
            .filter { !it.hidden }
            .groupBy { it.ssid }
            .forEach { (ssid, accessPoints) ->
                val securityTypes = accessPoints.map { it.security }.distinct()
                if (accessPoints.size > 1 && securityTypes.size > 1) {
                    val labels = securityTypes.joinToString(" / ") { it.label }
                    accessPoints.forEach { accessPoint ->
                        add(
                            Finding(
                                network = accessPoint,
                                kind = FindingKind.EVIL_TWIN,
                                detail = "\"$ssid\" is advertised by ${accessPoints.size} access points " +
                                    "with different security ($labels).",
                            ),
                        )
                    }
                }
            }

        networks.forEach { network ->
            when (network.security) {
                SecurityType.OPEN -> add(
                    Finding(
                        network = network,
                        kind = FindingKind.OPEN,
                        detail = "Devices on this network exchange unprotected traffic.",
                    ),
                )
                SecurityType.WEP, SecurityType.WPA -> add(
                    Finding(
                        network = network,
                        kind = FindingKind.WEAK_CRYPTO,
                        detail = "This network uses ${network.securityLabel}.",
                    ),
                )
                else -> Unit
            }

            if (network.hidden) {
                add(
                    Finding(
                        network = network,
                        kind = FindingKind.HIDDEN,
                        detail = "The network does not publish its name.",
                    ),
                )
            }
        }

        // One highest-severity finding per access point keeps the list readable.
        return candidates.values
            .mapNotNull { findings -> findings.maxByOrNull { it.kind.severity.rank } }
            .sortedWith(
                compareByDescending<Finding> { it.kind.severity.rank }
                    .thenByDescending { it.network.rssi },
            )
    }
}
