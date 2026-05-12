package com.fleet.bms.interfaces.ui.ecocar.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarColors

@Composable
fun LowBatteryDialog(
    onDismiss: () -> Unit,
    onNavigateToCharging: () -> Unit,
    onTechnicalIssues: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EcoCarColors.SurfaceElevated,
        titleContentColor = EcoCarColors.OnDark,
        textContentColor = EcoCarColors.OnDarkSecondary,
        title = {
            Text(
                text = "Niedriger Batterieladestand",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Battery low — es betrifft ggf. beide Systeme: Hochvoltbatterie und 12-V-Bordnetz.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Bitte wählen Sie eine nächste Aktion.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onNavigateToCharging()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EcoCarColors.GoldenYellow,
                        contentColor = EcoCarColors.NearBlack,
                    ),
                ) {
                    Text("Zur nächsten Solar-/Ladestation")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = {
                            onTechnicalIssues()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Technische Hinweise", color = EcoCarColors.GoldenYellow)
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Schließen", color = EcoCarColors.OnDarkSecondary)
                    }
                }
            }
        },
    )
}
