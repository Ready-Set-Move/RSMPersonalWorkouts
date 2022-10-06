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
import com.readysetmove.personalworkouts.device.DeviceConfiguration

@Composable
fun DeviceOverviewCard(deviceName: String, currentWeight: Float, deviceConfiguration: DeviceConfiguration?, onCalibrate: () -> Unit, onSetTara: () -> Unit, onReadSettings: () -> Unit) {
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
            Button(onClick = onReadSettings) {
                Text(text = "Read Settings")
            }
            Button(onClick = onCalibrate) {
                Text(text = "Calibrate")
            }
            Button(onClick = onSetTara) {0
                Text(text = "Set tara")
            }
            Spacer(Modifier.height(AppTheme.spacings.md))
            Text(text = "$deviceConfiguration", style = AppTheme.typography.body1)
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
            DeviceOverviewCard(
                "Your Device",
                1337.0f,
                deviceConfiguration = DeviceConfiguration("isoX"),
                onSetTara = {},
                onCalibrate = {},
                onReadSettings = {},
            )
        }
    }
}