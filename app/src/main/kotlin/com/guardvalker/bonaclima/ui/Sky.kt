package com.guardvalker.bonaclima.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.guardvalker.bonaclima.domain.Condition
import com.guardvalker.bonaclima.domain.Forecast
import java.time.Duration
import java.time.LocalDateTime

/** Estado visual del cielo derivado del pronóstico: colores del degradé + cuánta luz de día hay. */
data class SkyState(
    val top: Color,
    val bottom: Color,
    val condition: Condition,
    /** 0 = noche cerrada, 1 = día pleno; rampa suave alrededor de amanecer/atardecer. */
    val daylight: Float,
)

private const val RAMP_MIN = 45f

private val INK_DARK = Color(0xFF2E1D10)
private val INK_LIGHT = Color(0xFFFBF1DA)

fun skyFor(forecast: Forecast?, style: BonaStyle): SkyState {
    val cur = forecast?.current ?: return SkyState(style.nightTop, style.nightBottom, Condition.Clear, 0f)
    val today = forecast.daily.firstOrNull { it.date == cur.time.toLocalDate() }
    val sunrise = today?.sunrise
    val sunset = today?.sunset

    val daylight: Float
    val twilight: Float
    if (sunrise != null && sunset != null) {
        val dRise = minutes(sunrise, cur.time) // minutos desde el amanecer
        val dSet = minutes(cur.time, sunset) // minutos hasta el atardecer
        daylight = smooth(dRise / RAMP_MIN + 0.5f) * smooth(dSet / RAMP_MIN + 0.5f)
        val nearHorizon = minOf(kotlin.math.abs(dRise), kotlin.math.abs(dSet))
        twilight = (1f - nearHorizon / RAMP_MIN).coerceIn(0f, 1f)
    } else {
        daylight = if (cur.isDay) 1f else 0f
        twilight = 0f
    }

    var top = lerp(style.nightTop, style.dayTop, daylight)
    var bottom = lerp(style.nightBottom, style.dayBottom, daylight)
    top = lerp(top, style.twiTop, twilight * 0.75f)
    bottom = lerp(bottom, style.twiBottom, twilight * 0.75f)

    val gray = when (cur.condition) {
        Condition.Clear -> 0f
        Condition.PartlyCloudy -> 0.2f
        Condition.Cloudy -> 0.65f
        Condition.Fog -> 0.7f
        Condition.Drizzle, Condition.Rain -> 0.75f
        Condition.Snow -> 0.6f
        Condition.Thunderstorm -> 0.85f
    }
    // El gris se oscurece de noche para no quedar un cielo "lavado".
    val greyBottom = lerp(style.nightBottom, style.overcast, daylight)
    val greyTop = lerp(greyBottom, style.nightTop, 0.35f)
    return SkyState(lerp(top, greyTop, gray), lerp(bottom, greyBottom, gray), cur.condition, daylight)
}

/** Tinta oscura o clara, la que mejor contraste tenga con el cielo actual. */
fun heroInk(sky: SkyState): Color {
    val mid = lerp(sky.top, sky.bottom, 0.5f).luminance()
    return if (contrast(mid, INK_DARK.luminance()) >= contrast(mid, INK_LIGHT.luminance())) INK_DARK else INK_LIGHT
}

private fun contrast(a: Float, b: Float) = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)

private fun minutes(from: LocalDateTime, to: LocalDateTime) = Duration.between(from, to).toMinutes().toFloat()
private fun smooth(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
