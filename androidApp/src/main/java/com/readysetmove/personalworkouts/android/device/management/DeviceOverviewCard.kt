package com.readysetmove.personalworkouts.android.device.management

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.theme.AppTheme

@Composable
fun DeviceOverviewCard(deviceName: String, currentWeight: Float, onSetTara: () -> Unit) {
    Card {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacings.md)
        )
        {
            Text(text = deviceName, style = AppTheme.typography.h1)
            Spacer(Modifier.height(AppTheme.spacings.md))
            Text(text = "${"%.2f".format(currentWeight)} kg", style = AppTheme.typography.h2)
            Spacer(Modifier.height(AppTheme.spacings.md))
            Button(onClick = onSetTara) {
                Text(text = "Set tara")
            }
        }
    }
}

@Preview(name = "Light Mode", widthDp = 1024)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 320
)
@Composable
fun PreviewDeviceOverviewCard() {
    AppTheme {
        androidx.compose.material.Surface {
            DeviceOverviewCard("Your Device", 1337.0f, ) {}
        }
    }
}