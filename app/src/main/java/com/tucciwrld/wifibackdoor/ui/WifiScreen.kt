package com.tucciwrld.wifibackdoor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tucciwrld.wifibackdoor.WifiUiState
import com.tucciwrld.wifibackdoor.data.Finding
import com.tucciwrld.wifibackdoor.data.SecurityType
import com.tucciwrld.wifibackdoor.data.Severity
import com.tucciwrld.wifibackdoor.data.WifiNetwork

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScreen(
    state: WifiUiState,
    onScan: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        if (state.permissionGranted) onScan()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("WiFi Back Door", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(
                        onClick = onScan,
                        enabled = state.permissionGranted && !state.scanning,
                    ) {
                        if (state.scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan again")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (state.permissionGranted) onScan() else onRequestPermissions() },
                icon = {
                    Icon(
                        imageVector = if (state.permissionGranted) Icons.Default.Search else Icons.Default.Lock,
                        contentDescription = null,
                    )
                },
                text = { Text(if (state.permissionGranted) "Scan" else "Allow access") },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(state) }

            if (!state.permissionGranted) {
                item { PermissionCard(onRequestPermissions) }
            }

            state.message?.let { message ->
                item { MessageCard(message) }
            }

            if (state.findings.isNotEmpty()) {
                item { SectionTitle("Needs attention", state.findings.size) }
                items(state.findings, key = { "finding-${it.network.bssid}" }) { finding ->
                    FindingCard(finding)
                }
            }

            item { SectionTitle("Nearby networks", state.networks.size) }

            if (state.networks.isEmpty()) {
                item { EmptyCard(permissionGranted = state.permissionGranted) }
            } else {
                items(state.networks, key = { it.bssid }) { network ->
                    NetworkCard(network)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: WifiUiState) {
    val highCount = state.findings.count { it.kind.severity == Severity.HIGH }
    val headline = when {
        !state.permissionGranted -> "Access needed"
        !state.wifiEnabled -> "Wi-Fi is off"
        highCount > 0 -> "Back door risk detected"
        state.networks.isEmpty() -> "No networks yet"
        else -> "No back doors detected"
    }
    val body = when {
        !state.permissionGranted -> "Allow nearby Wi-Fi access to list the access points around you."
        !state.wifiEnabled -> "Turn Wi-Fi on, then scan again."
        state.networks.isEmpty() -> "Tap Scan to look for nearby access points."
        else -> "${state.networks.size} access points · ${state.findings.size} flagged"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(headline, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermissions: () -> Unit) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Why is this needed?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Android only reveals nearby Wi-Fi networks to apps with location or nearby-device " +
                    "access. Scans stay on this device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onRequestPermissions) {
                Text("Grant access")
            }
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyCard(permissionGranted: Boolean) {
    Card {
        Text(
            text = if (permissionGranted) {
                "No access points found yet. Tap Scan to refresh."
            } else {
                "Grant access to see the networks around you."
            },
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FindingCard(finding: Finding) {
    val accent = severityColor(finding.kind.severity)
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = accent,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = finding.kind.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
                Text(
                    text = finding.network.ssid,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = finding.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = finding.kind.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NetworkCard(network: WifiNetwork) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = network.ssid,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = network.bssid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SignalBars(network.signalBars)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecurityBadge(network)
                Text(
                    text = "${network.band.label} · ch ${network.channel} · ${network.signalLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SecurityBadge(network: WifiNetwork) {
    val secure = network.security != SecurityType.OPEN && network.security != SecurityType.WEP
    val container = if (secure) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (secure) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        Text(
            text = network.securityLabel,
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

@Composable
private fun SignalBars(bars: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(4) { index ->
            val active = index < bars
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height((6 + index * 4).dp)
                    .background(
                        color = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

private fun severityColor(severity: Severity): Color = when (severity) {
    Severity.HIGH -> Color(0xFFB3261E)
    Severity.MEDIUM -> Color(0xFFB26A00)
    Severity.LOW -> Color(0xFF3F6B8F)
}
