package com.readysetmove.personalworkouts.android.workout.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.device.Traction

@Composable
fun TractionsGraph(tractions: List<Traction>, modifier : Modifier) {
    if (tractions.isEmpty()) return

    val maxTraction = tractions.maxOf { it.value }
    val maxTimestamp = tractions.maxOf { it.timestamp }
    Box(modifier = modifier
        .background(Color.White)
        .padding(horizontal = AppTheme.spacings.sm, vertical = AppTheme.spacings.sm),
        contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            tractions.fold<Traction, Offset?>(initial = null) { prev, current ->
                val x = size.width * (current.timestamp.toFloat()/maxTimestamp.toFloat())
                val y = size.height * (current.value/maxTraction)
                val currentPoint = Offset(x=x,y=y)
                drawCircle(
                    color = Color.Red,
                    radius = 5f,
                    center = currentPoint
                )
                if (prev != null) {
                    drawLine(
                        color = Color.Black,
                        start = prev,
                        end = currentPoint,
                        strokeWidth = 2f,
                    )
                }
                return@fold currentPoint
            }
        }
    }
}