package com.readysetmove.personalworkouts.android.workout

import android.content.res.Configuration
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.readysetmove.personalworkouts.android.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun WorkingScreen(
    tractionGoal: Int,
    timeToWork: Long,
    timeGoal: Int,
    currentLoad: Float,
) {
    val loadPercent = currentLoad/tractionGoal
    val colorOver100Percent = if (loadPercent >= 1) (255*(loadPercent-1)).roundToInt() else 0
    val blueFraction = 255-(280*loadPercent).roundToInt()
    val redFraction = 0+colorOver100Percent*3
    val greenFraction = 255-colorOver100Percent*4
    val color = Color(
        red = if(redFraction < 255) redFraction else 255,
        green = if(greenFraction > 0) greenFraction else 0,
        blue = if(blueFraction > 0) blueFraction else 0
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val goalHeight = size.height/2
        val loadHeight =  goalHeight*loadPercent
        val rectHeight =  if (loadHeight<0) 0f else loadHeight
        drawRect(
            color = color,
            topLeft = Offset(0f, size.height-rectHeight),
            size = Size(size.width, rectHeight)
        )
        drawLine(
            start = Offset(0f, size.height/2-60),
            end = Offset(120f, size.height/2),
            color = Color.Black,
            strokeWidth = 5f,
        )
        drawLine(
            start = Offset(0f, size.height/2+60),
            end = Offset(120f, size.height/2),
            color = Color.Black,
            strokeWidth = 5f,
        )
        val paint = Paint()
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 48f
        paint.color = 0xffffffff.toInt()
        drawIntoCanvas {
            it.nativeCanvas.drawText("$tractionGoal", 0f, size.height/2+24f, paint)
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val timeProgress = timeToWork/(timeGoal*1000f)
        CircularProgressIndicator(
            progress = timeProgress,
            modifier = Modifier
                .width(400.dp)
                .padding(bottom = 400.dp), strokeWidth = 45.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${(currentLoad - tractionGoal).roundToInt()}", style = AppTheme.typography.h1)
            Text(text = "${if (timeToWork > 1000) ((timeToWork-1)/1000)+1 else "%.1f".format((timeToWork).toFloat()/1000)}s", style = AppTheme.typography.h2)
        }
    }
}

@Preview(
    name = "Light Mode Phone",
    widthDp = 480,
    showBackground = true,
)
@Preview(
    name = "Light Mode",
    widthDp = 1024,
    showBackground = true,
)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 1024
)
@Composable
fun WorkingScreenPreview() {
    AppTheme {
        WorkingScreen(
            tractionGoal = 50,
            timeToWork = 5100,
            currentLoad = 60f,
            timeGoal = 8,
        )
    }
}