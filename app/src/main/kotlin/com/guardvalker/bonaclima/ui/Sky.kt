package com.guardvalker.bonaclima.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

private val NIGHT_TOP = Color(0xFF070B1F)
private val NIGHT_BOTTOM = Color(0xFF1B2850)
private val DAY_TOP = Color(0xFF1E78D6)
private val DAY_BOTTOM = Color(0xFF7CC4FF)
private val TWI_TOP = Color(0xFF3A2A6E)
private val TWI_BOTTOM = Color(0xFFFF9466)
private val GREY_TOP = Color(0xFF4A5565)
private val GREY_BOTTOM = Color(0xFF8795A6)

private const val RAMP_MIN = 45f

fun skyFor(forecast: Forecast?): SkyState {
    val cur = forecast?.current ?: return SkyState(NIGHT_TOP, NIGHT_BOTTOM, Condition.Clear, 0f)
    val today = forecast.daily.firstOrNull { it.date == cur.time.toLocalDate() }
    val sunrise = today?.sunrise
    val sunset = today?.sunset

    val daylight: Float
    val twilight: Float
    if (sunrise != null && sunset != null) {
        val dRise = minutes(sunrise, cur.time) // minutos desde el amanecer
        val dSet = minutes(cur.time, sunset) // minutos hasta el atardecer
        val up = smooth(dRise / RAMP_MIN + 0.5f) * smooth(dSet / RAMP_MIN + 0.5f)
        daylight = up
        val nearHorizon = minOf(kotlin.math.abs(dRise), kotlin.math.abs(dSet))
        twilight = (1f - nearHorizon / RAMP_MIN).coerceIn(0f, 1f)
    } else {
        daylight = if (cur.isDay) 1f else 0f
        twilight = 0f
    }

    var top = lerp(NIGHT_TOP, DAY_TOP, daylight)
    var bottom = lerp(NIGHT_BOTTOM, DAY_BOTTOM, daylight)
    top = lerp(top, TWI_TOP, twilight * 0.75f)
    bottom = lerp(bottom, TWI_BOTTOM, twilight * 0.75f)

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
    val greyTop = lerp(Color(0xFF0E131C), GREY_TOP, daylight)
    val greyBottom = lerp(Color(0xFF263042), GREY_BOTTOM, daylight)
    return SkyState(lerp(top, greyTop, gray), lerp(bottom, greyBottom, gray), cur.condition, daylight)
}

private fun minutes(from: LocalDateTime, to: LocalDateTime) = Duration.between(from, to).toMinutes().toFloat()
private fun smooth(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
