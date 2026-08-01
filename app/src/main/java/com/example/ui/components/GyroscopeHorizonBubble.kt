package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.TiltData
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SpatialAmber
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun GyroscopeHorizonBubble(
    tiltData: TiltData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.width / 2f - 4f

                // Outer boundary circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = outerRadius,
                    style = Stroke(width = 3f)
                )

                // Crosshair lines
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(center.x - outerRadius, center.y),
                    end = Offset(center.x + outerRadius, center.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(center.x, center.y - outerRadius),
                    end = Offset(center.x, center.y + outerRadius),
                    strokeWidth = 2f
                )

                // Tilt position dot
                val maxOffset = outerRadius * 0.7f
                val dotX = center.x + (tiltData.roll * maxOffset)
                val dotY = center.y + (tiltData.pitch * maxOffset)

                val dotColor = if (tiltData.isHardwareSensorActive) NeonCyan else SpatialAmber

                drawCircle(
                    color = dotColor,
                    radius = 8f,
                    center = Offset(dotX, dotY)
                )

                // Glow ring around dot
                drawCircle(
                    color = dotColor.copy(alpha = 0.3f),
                    radius = 14f,
                    center = Offset(dotX, dotY)
                )
            }

            Text(
                text = if (tiltData.isHardwareSensorActive) {
                    "陀螺儀動態 (${(tiltData.roll * 30).roundToInt()}°, ${(tiltData.pitch * 30).roundToInt()}°)"
                } else {
                    "觸控 3D 傾斜 (${(tiltData.roll * 30).roundToInt()}°, ${(tiltData.pitch * 30).roundToInt()}°)"
                },
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
