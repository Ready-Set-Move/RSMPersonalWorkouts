package com.readysetmove.personalworkouts.android.device.management

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.readysetmove.personalworkouts.android.Exercise
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.Workout
import com.readysetmove.personalworkouts.android.device.Device
import com.readysetmove.personalworkouts.android.theme.AppTheme

@Composable
fun DeviceOverviewCard(device: Device, onClick: (device: Device) -> Unit) {
    Card(modifier = Modifier.clickable { onClick(device) }) {
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
            DeviceOverviewCard(device = Device(name = "Your Device")) {}
        }
    }
}