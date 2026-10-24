package com.guardvalker.bonaclima.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.guardvalker.bonaclima.domain.Condition
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private fun fract(x: Float) = x - floor(x)

private class Particle(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

private fun particles(n: Int, seed: Int) = Random(seed).let { r ->
    List(n) { Particle(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, r.nextFloat(), r.nextFloat() * 6.28f) }
}

/** Cielo animado: degradé + estrellas/sol/luna, nubes, lluvia, nieve, niebla y rayos según la condición. Todo se calcula del tiempo (sin estado por partícula). */
@Composable
fun WeatherBackground(sky: SkyState, modifier: Modifier = Modifier) {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t = (it - start) / 1_000_000_000f }
    }
    val stars = remember { particles(80, 1) }
    val clouds = remember { particles(10, 2) }
    val drops = remember { particles(170, 3) }
    val flakes = remember { particles(110, 4) }
    val bands = remember { particles(5, 5) }

    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(sky.top, sky.bottom)))
        val c = sky.condition
        val night = 1f - sky.daylight

        if (c == Condition.Clear || c == Condition.PartlyCloudy) {
            if (night > 0.05f) drawStars(stars, t, night)
            drawCelestial(sky.daylight, t)
        }
        val cloudCount = when (c) {
            Condition.Clear -> 2
            Condition.PartlyCloudy -> 5
            Condition.Fog -> 3
            else -> 10
        }
        drawClouds(clouds.take(cloudCount), t, sky.daylight, dark = c == Condition.Thunderstorm || c == Condition.Rain)

        when (c) {
            Condition.Rain -> drawRain(drops.take(130), t, 1f)
            Condition.Drizzle -> drawRain(drops.take(60), t, 0.6f)
            Condition.Thunderstorm -> { drawRain(drops, t, 1.2f); drawLightning(t) }
            Condition.Snow -> drawSnow(flakes, t)
            Condition.Fog -> drawFog(bands, t, sky.daylight)
            else -> {}
        }
    }
}

private fun DrawScope.drawStars(stars: List<Particle>, t: Float, night: Float) {
    for (s in stars) {
        val twinkle = 0.5f + 0.5f * sin(t * (0.8f + s.speed) + s.phase)
        drawCircle(
            Color.White.copy(alpha = (night * (0.25f + 0.65f * twinkle)).coerceIn(0f, 1f)),
            radius = (0.6f + s.size * 1.4f) * density,
            center = Offset(s.x * size.width, s.y * size.height * 0.6f),
        )
    }
}

private fun DrawScope.drawCelestial(daylight: Float, t: Float) {
    val center = Offset(size.width * 0.78f, size.height * 0.16f)
    val pulse = 1f + 0.06f * sin(t * 0.9f)
    if (daylight > 0.3f) {
        val a = ((daylight - 0.3f) / 0.7f).coerceIn(0f, 1f)
        val r = size.width * 0.5f * pulse
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFFFF3C4).copy(alpha = 0.85f * a), Color(0xFFFFD27A).copy(alpha = 0.25f * a), Color.Transparent), center, r),
            r, center,
        )
        drawCircle(Color(0xFFFFF8DC).copy(alpha = a), 26f * density * pulse, center)
    } else {
        val a = (1f - daylight / 0.3f).coerceIn(0f, 1f)
        val r = size.width * 0.28f * pulse
        drawCircle(Brush.radialGradient(listOf(Color(0xFFDDE6FF).copy(alpha = 0.35f * a), Color.Transparent), center, r), r, center)
        drawCircle(Color(0xFFF2F5FF).copy(alpha = 0.95f * a), 20f * density, center)
        drawCircle(Color(0xFF1B2850).copy(alpha = 0.55f * a), 18f * density, center + Offset(9f * density, -4f * density))
    }
}

private fun DrawScope.drawClouds(clouds: List<Particle>, t: Float, daylight: Float, dark: Boolean) {
    val base = if (dark) Color(0xFF2C3442) else Color.White
    val tint = androidx.compose.ui.graphics.lerp(Color(0xFF8C97B5), base, daylight)
    for (c in clouds) {
        val span = size.width * 1.6f
        val cx = fract(c.x + t * 0.012f * c.speed) * span - size.width * 0.3f
        val cy = size.height * (0.06f + c.y * 0.38f)
        val w = size.width * (0.28f + c.size * 0.22f)
        val alpha = (if (dark) 0.55f else 0.32f) * (0.7f + 0.3f * c.size)
        for (k in 0 until 5) {
            val ox = (k - 2) * w * 0.28f
            val oy = (if (k % 2 == 0) 0f else -0.12f) * w
            val r = w * (0.34f - kotlin.math.abs(k - 2) * 0.04f)
            val ctr = Offset(cx + ox, cy + oy)
            drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = alpha), Color.Transparent), ctr, r), r, ctr)
        }
    }
}

private fun DrawScope.drawRain(drops: List<Particle>, t: Float, intensity: Float) {
    val len = 16f * density * (0.8f + intensity * 0.3f)
    for (d in drops) {
        val fall = fract(d.y + t * (0.9f + d.speed * 0.7f))
        val y = fall * (size.height + len) - len
        val x = fract(d.x + fall * 0.08f) * size.width
        drawLine(
            Color(0xFFCFE3FF).copy(alpha = 0.35f + 0.25f * d.size),
            Offset(x, y), Offset(x - len * 0.18f, y + len),
            strokeWidth = (1f + d.size) * density * 0.8f, cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSnow(flakes: List<Particle>, t: Float) {
    for (f in flakes) {
        val fall = fract(f.y + t * 0.06f * (0.5f + f.speed))
        val x = (f.x + 0.025f * sin(t * 0.7f * f.speed + f.phase)).let { fract(it) } * size.width
        drawCircle(Color.White.copy(alpha = 0.55f + 0.4f * f.size), (1.5f + f.size * 3f) * density, Offset(x, fall * size.height))
    }
}

private fun DrawScope.drawFog(bands: List<Particle>, t: Float, daylight: Float) {
    val col = androidx.compose.ui.graphics.lerp(Color(0xFF8C97B5), Color.White, daylight)
    for (b in bands) {
        val h = size.height * 0.16f
        val y = size.height * (0.15f + b.y * 0.7f)
        val shift = (fract(b.x + t * 0.01f * b.speed) - 0.5f) * size.width
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, col.copy(alpha = 0.28f), Color.Transparent), y - h, y + h),
            topLeft = Offset(shift, y - h), size = androidx.compose.ui.geometry.Size(size.width * 1.2f, h * 2),
        )
    }
}

private fun DrawScope.drawLightning(t: Float) {
    val p = t % 7f
    val a = when {
        p < 0.12f -> 0.55f * (1f - p / 0.12f)
        p in 0.3f..0.4f -> 0.35f * (1f - (p - 0.3f) / 0.1f)
        else -> 0f
    }
    if (a > 0f) drawRect(Color.White.copy(alpha = a))
}
