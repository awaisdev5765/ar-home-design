package com.arhomedesign.ar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arhomedesign.ar.data.ArCoreDeviceStatus

@Composable
fun ArCoreStatusBanner(
    status: ArCoreDeviceStatus,
    onRefreshStatus: () -> Unit,
    onInstallArCore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unsupportedDevice = status.availabilityName == "UNSUPPORTED_DEVICE_NOT_CAPABLE"
    val bannerStyle = when {
        status.canOpenSession -> Triple(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Outlined.Info,
        )
        status.showInstallButton -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Outlined.Warning,
        )
        unsupportedDevice -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Outlined.Warning,
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Outlined.Info,
        )
    }
    val container = bannerStyle.first
    val content = bannerStyle.second
    val icon = bannerStyle.third

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null)
                Text(
                    text = status.headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = content
                )
            }
            Text(
                text = status.description,
                style = MaterialTheme.typography.bodyMedium,
                color = content.copy(alpha = 0.9f)
            )
            HorizontalDivider(color = content.copy(alpha = 0.18f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRefreshStatus,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = content)
                ) {
                    Text("Refresh status", style = MaterialTheme.typography.labelLarge)
                }
                if (status.showInstallButton) {
                    Button(
                        onClick = onInstallArCore,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Install / update", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
