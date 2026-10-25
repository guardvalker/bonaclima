package com.guardvalker.bonaclima.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import com.guardvalker.bonaclima.domain.Condition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Íconos dibujados a trazo de tinta sobre una grilla de 24x24 (sin emojis ni fuentes de íconos),
 * para que app y widget compartan el mismo estilo.
 */
enum class Glyph { Gear, Pin, Search, Refresh, Close, Back, Chevron, Drop, Wind, Gauge, Sun, Sunrise, Sunset, Book, Info, Check }

private val INK = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
fun WeatherGlyph(condition: Condition, isDay: Boolean, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        withTransform({ scale(k, k, Offset.Zero) }) { drawWeather(condition, isDay, color) }
    }
}

@Composable
fun UiGlyph(glyph: Glyph, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        withTransform({ scale(k, k, Offset.Zero) }) { drawGlyph(glyph, color) }
    }
}

private fun circlePath(cx: Float, cy: Float, r: Float) = Path().apply { addOval(Rect(Offset(cx, cy), r)) }

private fun cloudPath(dx: Float, dy: Float, k: Float = 1f): Path {
    fun c(cx: Float, cy: Float, r: Float) = circlePath(cx * k + dx, cy * k + dy, r * k)
    val body = Path().apply { addRect(Rect(8f * k + dx, 14.5f * k + dy, 16.8f * k + dx, 18f * k + dy)) }
    return listOf(c(8f, 14.5f, 3.5f), c(12.5f, 11f, 4.5f), c(16.8f, 14.8f, 3.2f), body)
        .reduce { a, b -> Path.combine(PathOperation.Union, a, b) }
}

private fun moonPath(cx: Float, cy: Float, r: Float) =
    Path.combine(PathOperation.Difference, circlePath(cx, cy, r), circlePath(cx + r * 0.55f, cy - r * 0.4f, r * 0.85f))

private fun DrawScope.line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color) =
    drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = INK.width, cap = StrokeCap.Round)

private fun DrawScope.rays(cx: Float, cy: Float, from: Float, to: Float, count: Int, color: Color, startDeg: Float = 0f, stepDeg: Float = 360f / count) {
    for (i in 0 until count) {
        val a = (startDeg + i * stepDeg) * PI.toFloat() / 180f
        line(cx + cos(a) * from, cy + sin(a) * from, cx + cos(a) * to, cy + sin(a) * to, color)
    }
}

private fun DrawScope.sunDisc(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(color, r, Offset(cx, cy), style = INK)
    rays(cx, cy, r + 2.4f, r + 4.6f, 8, color)
}

private fun DrawScope.drawWeather(c: Condition, day: Boolean, col: Color) {
    when (c) {
        Condition.Clear -> if (day) sunDisc(12f, 12f, 4.2f, col) else drawPath(moonPath(12f, 12f, 7.5f), col, style = INK)
        Condition.PartlyCloudy -> {
            val cloud = cloudPath(3.2f, 5f, 0.85f)
            clipPath(cloud, ClipOp.Difference) {
                if (day) {
                    drawCircle(col, 3.2f, Offset(8f, 8.5f), style = INK)
                    rays(8f, 8.5f, 5.3f, 7.2f, 8, col)
                } else {
                    drawPath(moonPath(8.5f, 8.5f, 4.8f), col, style = INK)
                }
            }
            drawPath(cloud, col, style = INK)
        }
        Condition.Cloudy -> drawPath(cloudPath(0f, 0f), col, style = INK)
        Condition.Fog -> {
            line(4f, 8f, 14f, 8f, col); line(7f, 12f, 20f, 12f, col)
            line(4f, 16f, 16f, 16f, col); line(9f, 20f, 20f, 20f, col)
        }
        Condition.Drizzle -> {
            drawPath(cloudPath(0f, -3.5f), col, style = INK)
            line(8.5f, 17f, 8f, 18.8f, col); line(13f, 17f, 12.5f, 18.8f, col)
            line(10.7f, 20f, 10.2f, 21.8f, col); line(15.2f, 20f, 14.7f, 21.8f, col)
        }
        Condition.Rain -> {
            drawPath(cloudPath(0f, -3.5f), col, style = INK)
            line(9f, 16.5f, 7.6f, 21f, col); line(13f, 16.5f, 11.6f, 21f, col); line(17f, 16.5f, 15.6f, 21f, col)
        }
        Condition.Snow -> {
            drawPath(cloudPath(0f, -3.5f), col, style = INK)
            for (x in listOf(8f, 12f, 16f)) {
                val y = if (x == 12f) 20.4f else 18.2f
                rays(x, y, 0f, 1.4f, 3, col, startDeg = 90f, stepDeg = 60f)
                rays(x, y, 0f, 1.4f, 3, col, startDeg = 270f, stepDeg = 60f)
            }
        }
        Condition.Thunderstorm -> {
            drawPath(cloudPath(0f, -3.5f), col, style = INK)
            val bolt = Path().apply {
                moveTo(13.2f, 13.5f); lineTo(10f, 18f); lineTo(13.6f, 18f); lineTo(11f, 22.5f)
            }
            drawPath(bolt, col, style = INK)
        }
    }
}

private fun DrawScope.drawGlyph(g: Glyph, col: Color) {
    when (g) {
        Glyph.Gear -> {
            drawCircle(col, 2.8f, Offset(12f, 12f), style = INK)
            drawCircle(col, 6.6f, Offset(12f, 12f), style = INK)
            rays(12f, 12f, 6.6f, 9.4f, 8, col)
        }
        Glyph.Pin -> {
            val tri = Path().apply { moveTo(6.9f, 12f); lineTo(17.1f, 12f); lineTo(12f, 21f); close() }
            drawPath(Path.combine(PathOperation.Union, circlePath(12f, 9.5f, 5.5f), tri), col, style = INK)
            drawCircle(col, 2f, Offset(12f, 9.5f), style = INK)
        }
        Glyph.Search -> {
            drawCircle(col, 5.6f, Offset(10.5f, 10.5f), style = INK)
            line(14.7f, 14.7f, 20f, 20f, col)
        }
        Glyph.Refresh -> {
            drawArc(col, -50f, 290f, false, Offset(5f, 5f), Size(14f, 14f), style = INK)
            val end = 240f * PI.toFloat() / 180f
            val p = Offset(12f + 7f * cos(end), 12f + 7f * sin(end))
            // tangente en el sentido de giro
            val dir = Offset(-sin(end), cos(end))
            for (s in listOf(-1f, 1f)) {
                val a = s * 0.55f
                val bx = -dir.x * cos(a) + dir.y * sin(a)
                val by = -dir.x * sin(a) - dir.y * cos(a)
                line(p.x, p.y, p.x + bx * 3.8f, p.y + by * 3.8f, col)
            }
        }
        Glyph.Close -> { line(6f, 6f, 18f, 18f, col); line(18f, 6f, 6f, 18f, col) }
        Glyph.Back -> { line(19f, 12f, 5f, 12f, col); line(11f, 6f, 5f, 12f, col); line(11f, 18f, 5f, 12f, col) }
        Glyph.Chevron -> {
            drawPath(Path().apply { moveTo(9f, 5.5f); lineTo(15.5f, 12f); lineTo(9f, 18.5f) }, col, style = INK)
        }
        Glyph.Drop -> {
            val p = Path().apply {
                moveTo(12f, 3.5f)
                cubicTo(15.5f, 8f, 18f, 11f, 18f, 14.5f)
                cubicTo(18f, 18f, 15.3f, 20.5f, 12f, 20.5f)
                cubicTo(8.7f, 20.5f, 6f, 18f, 6f, 14.5f)
                cubicTo(6f, 11f, 8.5f, 8f, 12f, 3.5f)
                close()
            }
            drawPath(p, col, style = INK)
        }
        Glyph.Wind -> {
            val a = Path().apply { moveTo(3f, 9f); lineTo(13f, 9f); cubicTo(16.5f, 9f, 16.5f, 4.8f, 13.5f, 4.8f) }
            val b = Path().apply { moveTo(3f, 13f); lineTo(18f, 13f); cubicTo(21.5f, 13f, 21.5f, 17.5f, 18.5f, 17.5f) }
            drawPath(a, col, style = INK); drawPath(b, col, style = INK)
            line(3f, 17f, 10f, 17f, col)
        }
        Glyph.Gauge -> {
            drawArc(col, 180f, 180f, false, Offset(4f, 6f), Size(16f, 16f), style = INK)
            line(4f, 14f, 20f, 14f, col)
            line(12f, 14f, 15.6f, 9f, col)
        }
        Glyph.Sun -> sunDisc(12f, 12f, 4.2f, col)
        Glyph.Sunrise, Glyph.Sunset -> {
            line(3f, 17.5f, 21f, 17.5f, col)
            drawArc(col, 180f, 180f, false, Offset(7f, 12.5f), Size(10f, 10f), style = INK)
            rays(12f, 17.5f, 7.4f, 9.4f, 3, col, startDeg = 200f, stepDeg = 70f)
            val up = g == Glyph.Sunrise
            line(12f, 3.5f, 12f, 9f, col)
            if (up) { line(12f, 3.5f, 9.6f, 6f, col); line(12f, 3.5f, 14.4f, 6f, col) }
            else { line(12f, 9f, 9.6f, 6.5f, col); line(12f, 9f, 14.4f, 6.5f, col) }
        }
        Glyph.Book -> {
            val p = Path().apply {
                moveTo(12f, 7f)
                cubicTo(10f, 5.5f, 6.5f, 5.5f, 4f, 6.2f); lineTo(4f, 18.5f)
                cubicTo(6.5f, 17.8f, 10f, 17.8f, 12f, 19.3f)
                cubicTo(14f, 17.8f, 17.5f, 17.8f, 20f, 18.5f); lineTo(20f, 6.2f)
                cubicTo(17.5f, 5.5f, 14f, 5.5f, 12f, 7f)
                lineTo(12f, 19.3f)
            }
            drawPath(p, col, style = INK)
        }
        Glyph.Info -> {
            drawCircle(col, 8.6f, Offset(12f, 12f), style = INK)
            line(12f, 11f, 12f, 16.5f, col)
            drawCircle(col, 0.5f, Offset(12f, 7.9f), style = INK)
        }
        Glyph.Check -> drawPath(Path().apply { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }, col, style = INK)
    }
}
