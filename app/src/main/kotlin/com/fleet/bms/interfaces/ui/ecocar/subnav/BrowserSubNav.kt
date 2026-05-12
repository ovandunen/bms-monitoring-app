package com.fleet.bms.interfaces.ui.ecocar.subnav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarColors

@Composable
fun BrowserSubNav(modifier: Modifier = Modifier) {
    var urlInput by rememberSaveable { mutableStateOf("https://duckduckgo.com/") }
    var loadedUrl by rememberSaveable { mutableStateOf("https://duckduckgo.com/") }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Browser",
            style = MaterialTheme.typography.titleMedium,
            color = EcoCarColors.OnDark,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HorizontalDivider(color = EcoCarColors.Divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                singleLine = true,
                label = { Text("URL") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = EcoCarColors.OnDark,
                    unfocusedTextColor = EcoCarColors.OnDark,
                    focusedBorderColor = EcoCarColors.GoldenYellow,
                    unfocusedBorderColor = EcoCarColors.Divider,
                    focusedLabelColor = EcoCarColors.GoldenYellow,
                    unfocusedLabelColor = EcoCarColors.OnDarkSecondary,
                ),
            )
            Button(onClick = { loadedUrl = urlInput.trim() }) {
                Text("Los")
            }
        }
        Text(
            text = "Nur HTTPS empfohlen. Inhalt von Drittanbietern beachten.",
            style = MaterialTheme.typography.bodySmall,
            color = EcoCarColors.OnDarkSecondary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        key(loadedUrl) {
            EcoWebView(
                url = loadedUrl,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}
