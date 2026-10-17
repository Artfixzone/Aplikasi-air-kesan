package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean vector-drawn multi-color Google 'G' logo
 */
@Composable
fun GoogleLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = w / 2f

        // Blue (#4285F4), Red (#EA4335), Yellow (#FBBC05), Green (#34A853)
        val blue = Color(0xFF4285F4)
        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        val innerRadius = radius * 0.55f

        // Red segment (Top)
        val redPath = Path().apply {
            moveTo(center.x, center.y)
            arcTo(
                rect = Rect(Offset.Zero, Size(w, h)),
                startAngleDegrees = -140f,
                sweepAngleDegrees = 95f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(redPath, red, style = Fill)

        // Blue segment (Right + horizontal bar)
        val bluePath = Path().apply {
            moveTo(center.x, center.y)
            arcTo(
                rect = Rect(Offset.Zero, Size(w, h)),
                startAngleDegrees = -45f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(bluePath, blue, style = Fill)

        // Green segment (Bottom)
        val greenPath = Path().apply {
            moveTo(center.x, center.y)
            arcTo(
                rect = Rect(Offset.Zero, Size(w, h)),
                startAngleDegrees = 45f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(greenPath, green, style = Fill)

        // Yellow segment (Left)
        val yellowPath = Path().apply {
            moveTo(center.x, center.y)
            arcTo(
                rect = Rect(Offset.Zero, Size(w, h)),
                startAngleDegrees = 135f,
                sweepAngleDegrees = 85f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(yellowPath, yellow, style = Fill)

        // Center cutout
        drawCircle(
            color = Color.White,
            radius = innerRadius,
            center = center
        )

        // Right horizontal bar for G
        val barHeight = radius * 0.38f
        drawRect(
            color = blue,
            topLeft = Offset(center.x - radius * 0.05f, center.y - barHeight / 2f),
            size = Size(radius * 1.05f, barHeight)
        )
    }
}
