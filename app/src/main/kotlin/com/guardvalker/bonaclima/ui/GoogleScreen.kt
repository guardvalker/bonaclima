package com.guardvalker.bonaclima.ui

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardvalker.bonaclima.domain.DailyPoint
import com.guardvalker.bonaclima.domain.Forecast
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val AR = Locale.forLanguageTag("es-AR")
private val HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val CARD = RoundedCornerShape(28.dp)
private val HERO = RoundedCornerShape(36.dp)

/** Esquema Material You (colores del fondo de pantalla) en Android 12+, uno azul fijo en versiones viejas. */
@Composable
fun googleScheme(dark: Boolean): ColorScheme {
    val ctx = LocalContext.current
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFFA9C7FF), background = Color(0xFF111318), surface = Color(0xFF111318), surfaceContainer = Color(0xFF1D2024), onSurface = Color(0xFFE2E2E9), onSurfaceVariant = Color(0xFFC4C6D0), outline = Color(0xFF8E9099))
        else -> lightColorScheme(primary = Color(0xFF3B6FD4), background = Color(0xFFF8F9FC), surface = Color(0xFFF8F9FC), surfaceContainer = Color(0xFFECEEF4), onSurface = Color(0xFF1B1C1F), onSurfaceVariant = Color(0xFF44474F), outline = Color(0xFF74777F))
    }
}

/** Pasa los colores del esquema al [BonaStyle] para que Ajustes, búsqueda y el cielo hereden la misma paleta. */
fun googleStyle(base: BonaStyle, s: ColorScheme, dark: Boolean) = base.copy(
    dark = dark, paper = s.background, card = s.surfaceContainer, ink = s.onSurface, inkSoft = s.onSurfaceVariant,
    accent = s.primary, line = s.outline.copy(alpha = 0.4f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMain(state: UiState, version: String, style: BonaStyle, onSearch: () -> Unit, onSettings: () -> Unit, onRefresh: () -> Unit) {
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(50)).clickable(onClick = onSearch).padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UiGlyph(Glyph.Pin, style.accent, 22.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(state.place.name, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = style.ink, maxLines = 1)
                }
                IconTap(Glyph.Gear, onSettings)
            }
            val f = state.forecast
            if (f == null) {
                Spacer(Modifier.height(120.dp))
                Text(state.error ?: "Cargando…", color = style.inkSoft, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp)
                return@Column
            }
            val sky = skyFor(f, style)
            GoogleHero(f, sky, style)
            if (state.error != null) Text(state.error, color = style.accent, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            BriefCard(f, style)
            GoogleHourly(f, style)
            GoogleDaily(f, style)
            GoogleDetails(f, style)
            Text(
                "BonaClima v$version · Datos: Open-Meteo · actualizado ${updatedAgo(f)}",
                color = style.inkSoft, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GCard(title: String?, style: BonaStyle, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(CARD).background(style.card).padding(18.dp)) {
        if (title != null) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = style.inkSoft)
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

@Composable
private fun GoogleHero(f: Forecast, sky: SkyState, style: BonaStyle) {
    val ink = heroInk(sky)
    val c = f.current
    val today = f.daily.firstOrNull()
    val t by rememberInfiniteTransition(label = "hero").animateFloat(
        0f, (2 * PI).toFloat() * 20, infiniteRepeatable(tween(120_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Box(Modifier.fillMaxWidth().height(300.dp).clip(HERO)) {
        WeatherBackground(sky, style, ink)
        FlatScene(c.condition, c.isDay, { t }, Modifier.align(Alignment.TopEnd).size(190.dp).padding(top = 8.dp, end = 4.dp))
        Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.Bottom) {
            Text("${c.temperature.roundToInt()}°", fontSize = 104.sp, fontWeight = FontWeight.Light, color = ink, lineHeight = 104.sp)
            Text(c.condition.label, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = ink)
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    if (today != null) append("Máx ${today.tempMax.roundToInt()}° · Mín ${today.tempMin.roundToInt()}° · ")
                    append("Sensación ${c.apparentTemperature.roundToInt()}°")
                },
                fontSize = 15.sp, color = ink.copy(alpha = 0.9f),
            )
        }
    }
}

/** Resumen en una frase armado con reglas (no es IA): condición actual, lluvia próxima y máxima del día. */
private fun briefText(f: Forecast): String {
    val c = f.current
    val now = c.time.withMinute(0).withSecond(0).withNano(0)
    val next = f.hourly.filter { it.time.isAfter(now) }.take(12)
    val rainy = next.firstOrNull { (it.precipProbability ?: 0) >= 50 }
    val today = f.daily.firstOrNull()
    return buildList {
        add("${c.condition.label} ahora, ${c.temperature.roundToInt()}°.")
        if (rainy != null) add("Probabilidad de lluvia de ${rainy.precipProbability}% a las ${rainy.time.hour} h.")
        else if (next.isNotEmpty()) add("Sin lluvia prevista en las próximas 12 horas.")
        if (today != null) add("Hoy llega a ${today.tempMax.roundToInt()}° y baja a ${today.tempMin.roundToInt()}°.")
        c.uvIndex?.takeIf { it >= 6 && c.isDay }?.let { add("UV ${uvLabel(it).substringBefore(" (").lowercase()}: usá protector.") }
    }.joinToString(" ")
}

@Composable
private fun BriefCard(f: Forecast, style: BonaStyle) {
    GCard("Resumen", style) { Text(briefText(f), fontSize = 17.sp, lineHeight = 24.sp, color = style.ink) }
}

@Composable
private fun GoogleHourly(f: Forecast, style: BonaStyle) {
    val now = f.current.time.withMinute(0).withSecond(0).withNano(0)
    val hours = f.hourly.filter { !it.time.isBefore(now) }.take(24)
    if (hours.size < 2) return
    val lo = hours.minOf { it.temperature }
    val hi = hours.maxOf { it.temperature }
    val span = (hi - lo).coerceAtLeast(1.0)
    val colW = 58.dp
    val chartH = 64.dp
    GCard("Por hora", style) {
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            val density = LocalDensity.current
            Canvas(Modifier.width(colW * hours.size).height(190.dp)) {
                val colPx = colW.toPx()
                val top = 100.dp.toPx()
                val h = chartH.toPx()
                val pts = hours.mapIndexed { i, p -> Offset(colPx * (i + .5f), top + h - ((p.temperature - lo) / span).toFloat() * h) }
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        val a = pts[i - 1]; val b = pts[i]
                        cubicTo((a.x + b.x) / 2, a.y, (a.x + b.x) / 2, b.y, b.x, b.y)
                    }
                }
                drawPath(path, style.accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                pts.forEach { drawCircle(style.accent, 4.dp.toPx(), it) }
            }
            Row {
                hours.forEachIndexed { i, p ->
                    val y = ((p.temperature - lo) / span).toFloat()
                    Column(Modifier.width(colW), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (i == 0) "Ahora" else HOUR_FMT.format(p.time), fontSize = 12.sp, color = style.inkSoft)
                        Spacer(Modifier.height(6.dp))
                        ColorIcon(p.condition, p.isDay, 30.dp)
                        val pr = p.precipProbability ?: 0
                        Text(if (pr >= 10) "$pr%" else " ", fontSize = 11.sp, color = style.water)
                        Spacer(Modifier.height(chartH * (1f - y) + 9.dp))
                        Text("${p.temperature.roundToInt()}°", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = style.ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorIcon(condition: com.guardvalker.bonaclima.domain.Condition, isDay: Boolean, size: Dp) {
    FlatScene(condition, isDay, { 0f }, Modifier.size(size))
}

@Composable
private fun GoogleDaily(f: Forecast, style: BonaStyle) {
    val days = f.daily.take(10)
    if (days.isEmpty()) return
    val lo = days.minOf { it.tempMin }
    val hi = days.maxOf { it.tempMax }
    GCard("Próximos ${days.size} días", style) {
        Column {
            days.forEachIndexed { i, d -> GDayRow(d, i == 0, lo, hi, style) }
        }
    }
}

@Composable
private fun GDayRow(d: DailyPoint, isToday: Boolean, lo: Double, hi: Double, style: BonaStyle) {
    val span = (hi - lo).coerceAtLeast(1.0)
    val start = ((d.tempMin - lo) / span).toFloat().coerceIn(0f, 1f)
    val width = ((d.tempMax - d.tempMin) / span).toFloat().coerceIn(0.05f, 1f - start)
    Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (isToday) "Hoy" else d.date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, AR).replaceFirstChar { it.uppercase() }.trimEnd('.'),
            modifier = Modifier.width(44.dp), fontSize = 16.sp, color = style.ink,
        )
        ColorIcon(d.condition, true, 30.dp)
        val p = d.precipProbability ?: 0
        Text(if (p >= 10) "$p%" else "", modifier = Modifier.width(40.dp).padding(start = 6.dp), fontSize = 12.sp, color = style.water)
        Text("${d.tempMin.roundToInt()}°", modifier = Modifier.width(32.dp), textAlign = TextAlign.End, fontSize = 15.sp, color = style.inkSoft)
        Row(Modifier.weight(1f).padding(horizontal = 8.dp).height(6.dp).clip(RoundedCornerShape(50)).background(style.line.copy(alpha = 0.35f))) {
            if (start > 0f) Spacer(Modifier.weight(start))
            Box(Modifier.weight(width).height(6.dp).clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(listOf(style.water, style.accent))))
            val rest = 1f - start - width
            if (rest > 0.001f) Spacer(Modifier.weight(rest))
        }
        Text("${d.tempMax.roundToInt()}°", modifier = Modifier.width(34.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = style.ink)
    }
}

@Composable
private fun GoogleDetails(f: Forecast, style: BonaStyle) {
    val c = f.current
    val today = f.daily.firstOrNull()
    val cards = buildList<Triple<String, String, String?>> {
        today?.let { d ->
            val mm = d.precipSumMm
            add(Triple("Precipitación", if (mm != null) "${fmt1(mm)} mm" else "${d.precipProbability ?: 0}%", "Probabilidad ${d.precipProbability ?: 0}% hoy"))
        }
        c.windSpeedKmh?.let { add(Triple("Viento", "${it.roundToInt()} km/h ${c.windDirectionDeg?.let(::compass).orEmpty()}", c.windGustKmh?.let { g -> "Ráfagas ${g.roundToInt()} km/h" })) }
        c.uvIndex?.let { add(Triple("Índice UV", uvLabel(it), today?.uvMax?.let { m -> "Máximo hoy ${m.roundToInt()}" })) }
        c.humidity?.let { add(Triple("Humedad", "$it%", c.dewPoint?.let { d -> "Punto de rocío ${d.roundToInt()}°" })) }
        f.airQuality?.let { add(Triple("Calidad del aire", "${it.usAqi} · ${aqiLabel(it.usAqi)}", it.pm25?.let { p -> "PM2.5 ${fmt1(p)} µg/m³" })) }
        c.visibilityKm?.let { add(Triple("Visibilidad", "${fmt1(it)} km", c.cloudCover?.let { n -> "Nubosidad $n%" })) }
        c.pressureHpa?.let { add(Triple("Presión", "${it.roundToInt()} hPa", null)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (k, v, sub) ->
                    GCard(k, style, Modifier.weight(1f)) {
                        Text(v, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = style.ink)
                        if (sub != null) Text(sub, fontSize = 12.sp, color = style.inkSoft, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (today?.sunrise != null && today.sunset != null) SunCard(f, style)
    }
}

/** Arco del sol entre amanecer y atardecer, con el sol en la posición actual. */
@Composable
private fun SunCard(f: Forecast, style: BonaStyle) {
    val d = f.daily.first()
    val rise = d.sunrise!!
    val set = d.sunset!!
    val span = java.time.Duration.between(rise, set).toMinutes().coerceAtLeast(1)
    val p = (java.time.Duration.between(rise, f.current.time).toMinutes().toFloat() / span).coerceIn(0f, 1f)
    val up = f.current.time.isAfter(rise) && f.current.time.isBefore(set)
    GCard("Sol", style) {
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val r = size.width * 0.38f
            val cx = size.width / 2f
            val base = size.height - 6.dp.toPx()
            drawLine(style.line, Offset(0f, base), Offset(size.width, base), 1.5.dp.toPx())
            val path = Path()
            for (i in 0..60) {
                val a = PI.toFloat() * (1f - i / 60f)
                val pt = Offset(cx + cos(a) * r, base - sin(a) * (size.height - 16.dp.toPx()))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, style.line, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            if (up) {
                val a = PI.toFloat() * (1f - p)
                val pt = Offset(cx + cos(a) * r, base - sin(a) * (size.height - 16.dp.toPx()))
                drawCircle(Color(0xFFFFC83D).copy(alpha = .3f), 14.dp.toPx(), pt)
                drawCircle(Color(0xFFFFC83D), 8.dp.toPx(), pt)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Amanecer ${HOUR_FMT.format(rise)}", fontSize = 14.sp, color = style.ink, modifier = Modifier.weight(1f))
            Text("Atardecer ${HOUR_FMT.format(set)}", fontSize = 14.sp, color = style.ink, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

private fun fmt1(x: Double) = String.format(AR, "%.1f", x)

private fun aqiLabel(aqi: Int) = when {
    aqi <= 50 -> "Buena"
    aqi <= 100 -> "Moderada"
    aqi <= 150 -> "Dañina p/ sensibles"
    aqi <= 200 -> "Dañina"
    aqi <= 300 -> "Muy dañina"
    else -> "Peligrosa"
}
