package com.readysetmove.personalworkouts.android.device.management

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.bluetooth.Device

@Composable
fun DeviceOverviewCard(device: Device) {
    Card {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacings.md)
        )
        {
            Text(text = device.name, style = AppTheme.typography.h1)
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
            DeviceOverviewCard(Device(name = "Your Device"))
        }
    }
}

@Preview(name = "Light Mode Selected", widthDp = 1024)
@Preview(
    name = "Dark Mode Selected",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 540
)
@Composable
fun PreviewSelectedDeviceOverviewCard() {
    AppTheme {
        androidx.compose.material.Surface {
            DeviceOverviewCard(Device(name = "Your Device"))
        }
    }
}