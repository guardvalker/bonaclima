package com.guardvalker.bonaclima.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardvalker.bonaclima.domain.Condition
import com.guardvalker.bonaclima.domain.Forecast
import com.guardvalker.bonaclima.domain.HourlyPoint
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private val AR = Locale.forLanguageTag("es-AR")

// Paleta de las franjas (la de la referencia): cada momento del día tiene su color.
private val C_MORNING = Color(0xFFE6C597)
private val C_AFTERNOON = Color(0xFFD99560)
private val C_DUSK = Color(0xFF8E4B40)
private val C_NIGHT = Color(0xFF4F3C3E)
private val INK_DARK = Color(0xFF2B2420)
private val INK_CREAM = Color(0xFFFFF3DE)

private enum class Period(val label: String, val color: Color, val repHour: Float) {
    Morning("Mañana", C_MORNING, 9f),
    Afternoon("Tarde", C_AFTERNOON, 15f),
    Dusk("Tardecita", C_DUSK, 19.5f),
    Night("Noche", C_NIGHT, 23f),
}

/** Cuánto está abierto cada módulo a una hora (suma 1): se abre el de la franja actual y se cierra el anterior de a poco. */
private val OPEN_STOPS = listOf(0f to 3, 5f to 3, 9f to 0, 15f to 1, 19.5f to 2, 23f to 3, 24f to 3)

private fun openness(hour: Float): FloatArray {
    val w = FloatArray(4)
    val i = OPEN_STOPS.indexOfLast { it.first <= hour }.coerceIn(0, OPEN_STOPS.size - 2)
    val (h0, a) = OPEN_STOPS[i]
    val (h1, b) = OPEN_STOPS[i + 1]
    val k = ((hour - h0) / (h1 - h0)).coerceIn(0f, 1f)
    w[a] += 1 - k
    w[b] += k
    return w
}

private fun inkOn(c: Color) = if (c.luminance() > 0.33f) INK_DARK else INK_CREAM

/** El clima en un instante: temperatura interpolada entre las horas del pronóstico y condición de la hora más cercana. */
private class Moment(val temp: Double, val condition: Condition, val isDay: Boolean, val precip: Int?)

private fun momentAt(f: Forecast, hour: Float): Moment {
    val date = f.current.time.toLocalDate()
    val pts = f.hourly.filter { it.time.toLocalDate() == date }.sortedBy { it.time }
    if (pts.isEmpty()) return Moment(f.current.temperature, f.current.condition, f.current.isDay, null)
    fun h(p: HourlyPoint) = p.time.hour + p.time.minute / 60f
    val after = pts.firstOrNull { h(it) >= hour } ?: pts.last()
    val before = pts.lastOrNull { h(it) <= hour } ?: pts.first()
    val span = h(after) - h(before)
    val k = if (span <= 0f) 0f else ((hour - h(before)) / span).coerceIn(0f, 1f)
    val near = if (k < 0.5f) before else after
    return Moment(before.temperature + (after.temperature - before.temperature) * k, near.condition, near.isDay, near.precipProbability)
}

private fun hourLabel(hour: Float): String {
    val h = floor(hour).toInt().coerceIn(0, 23)
    val m = ((hour - floor(hour)) * 60).toInt().coerceIn(0, 59)
    return "%02d:%02d".format(h, m)
}

private val STEP = 0.35f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandsMain(state: UiState, version: String, style: BonaStyle, onSearch: () -> Unit, onSettings: () -> Unit, onRefresh: () -> Unit) {
    val f = state.forecast
    val clock = rememberInfiniteTransition(label = "clock").animateFloat(
        0f, 100_000f, infiniteRepeatable(tween(100_000_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Column(Modifier.fillMaxSize()) {
        // Cabecera fija: no se mete bajo la barra de estado al scrollear.
        Row(Modifier.fillMaxWidth().background(style.paper).statusBarsPadding().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClick = onSearch).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UiGlyph(Glyph.Pin, style.accent, 20.dp)
                Spacer(Modifier.width(8.dp))
                Text(state.place.name, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = style.ink, maxLines = 1)
            }
            IconTap(Glyph.Gear, onSettings)
        }
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f).fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val screenH = maxHeight
                val screenW = maxWidth
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    if (f == null) {
                        Text(
                            state.error ?: "Cargando…", color = style.inkSoft, fontStyle = FontStyle.Italic,
                            modifier = Modifier.fillMaxWidth().padding(top = 120.dp), textAlign = TextAlign.Center, fontSize = 16.sp,
                        )
                        return@Column
                    }
                    val nowHour = f.current.time.hour + f.current.time.minute / 60f
                    val currentIdx = openness(nowHour).let { w -> w.indices.maxBy { w[it] } }
                    val scope = rememberCoroutineScope()
                    val hourAnim = remember { Animatable(nowHour) }
                    // null = mostrando el momento actual; si no, índice de la franja elegida.
                    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
                    var hinted by rememberSaveable { mutableStateOf(false) }
                    val scrubbing = selected != null
                    val hour = if (scrubbing) hourAnim.value else nowHour

                    fun hourOf(idx: Int) = if (idx == currentIdx) nowHour else Period.entries[idx].repHour
                    fun select(idx: Int) {
                        val from = selected
                        selected = if (idx == currentIdx) null else idx
                        scope.launch {
                            if (from == null) hourAnim.snapTo(nowHour)
                            hourAnim.animateTo(hourOf(idx), tween(380))
                        }
                    }

                    val haptic = LocalHapticFeedback.current
                    LaunchedEffect(selected) { if (selected != null) haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) }

                    val thresholdPx = with(LocalDensity.current) { 44.dp.toPx() }
                    val w = openness(hour)
                    val dominant = w.indices.maxBy { w[it] }
                    val expandedH = screenH * 0.32f
                    val collapsedH = 46.dp
                    val numberW = screenW - 40.dp - 128.dp - 8.dp

                    // Deslizar sobre las franjas "corre" el módulo abierto: un gesto mueve solo hasta la franja contigua, con snap.
                    Column(
                        Modifier.fillMaxWidth().pointerInput(currentIdx, nowHour) {
                            var acc = 0f
                            var fired = false
                            detectVerticalDragGestures(
                                onDragStart = { acc = 0f; fired = false },
                                onDragEnd = { if (!fired) select(selected ?: currentIdx) },
                                onDragCancel = { if (!fired) select(selected ?: currentIdx) },
                            ) { change, dy ->
                                change.consume()
                                hinted = true
                                if (fired) return@detectVerticalDragGestures
                                acc += dy
                                val base = selected ?: currentIdx
                                // Dedo hacia abajo = franja anterior, hacia arriba = la siguiente.
                                val dir = if (acc > 0) -1 else 1
                                val target = base + dir
                                if (kotlin.math.abs(acc) >= thresholdPx && target in 0..3) {
                                    fired = true
                                    select(target)
                                } else if (target in 0..3) {
                                    val k = (kotlin.math.abs(acc) / thresholdPx).coerceIn(0f, 1f) * STEP
                                    val h0 = hourOf(base)
                                    scope.launch {
                                        if (selected == null) { selected = base; hourAnim.snapTo(h0) }
                                        hourAnim.snapTo(h0 + (hourOf(target) - h0) * k)
                                    }
                                }
                            }
                        },
                    ) {
                        Period.entries.forEachIndexed { idx, p ->
                            val live = idx == dominant
                            val m = if (live && !scrubbing) Moment(f.current.temperature, f.current.condition, f.current.isDay, null)
                            else momentAt(f, if (live) hour else p.repHour)
                            Module(
                                p, m, f, w[idx], live, scrubbing && live, hinted, hour, clock, collapsedH, expandedH, numberW,
                                onJump = { select(idx) },
                                onReset = { select(currentIdx) },
                            )
                        }
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        HourlyCard(f, style)
                        DailyCard(f, style)
                        DetailsGrid(f, style)
                        Text(
                            "BonaClima v$version · Datos: Open-Meteo · actualizado ${updatedAgo(f)}",
                            color = style.inkSoft, fontSize = 11.sp, fontStyle = FontStyle.Italic,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** Módulo de un momento del día: plegado es una franja fina; abierto (open 0..1) muestra número, clima y escena animada. */
@Composable
private fun Module(
    p: Period, m: Moment, f: Forecast, open: Float, live: Boolean, scrubbing: Boolean, hinted: Boolean, hour: Float,
    clock: State<Float>, collapsedH: Dp, expandedH: Dp, numberW: Dp, onJump: () -> Unit, onReset: () -> Unit,
) {
    val ink = inkOn(p.color)
    val soft = ink.copy(alpha = 0.78f)
    val today = f.daily.firstOrNull()
    // Los dos contenidos no se superponen: el plegado se apaga en la primera mitad y el abierto aparece en la segunda.
    val foldedA = (1f - open * 2f).coerceIn(0f, 1f)
    val openA = (open * 2f - 1f).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(collapsedH + (expandedH - collapsedH) * open).clip(RoundedCornerShape(0.dp)).background(p.color).clickable(enabled = open < 0.5f, onClick = onJump)) {
        if (foldedA > 0f) {
            Row(
                Modifier.fillMaxWidth().height(collapsedH).padding(horizontal = 20.dp).alpha(foldedA),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(p.label.uppercase(AR), fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.85f), maxLines = 1, modifier = Modifier.weight(1f))
                WeatherGlyph(m.condition, m.isDay, ink, 22.dp)
                Spacer(Modifier.width(12.dp))
                Text("${m.temp.roundToInt()}°", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = ink, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            }
        }
        if (openA > 0f) {
            val numberSp = (numberW.value / 1.75f).coerceIn(56f, ((expandedH.value - 40f) * 0.7f).coerceAtLeast(56f))
            Column(Modifier.fillMaxWidth().height(expandedH).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp).alpha(openA)) {
                Text(
                    when {
                        scrubbing -> "${p.label} · ${hourLabel(hour)} · ↺ AHORA"
                        live -> "${p.label} · AHORA"
                        else -> p.label
                    }.uppercase(AR),
                    fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold, color = soft, maxLines = 1, softWrap = false,
                    modifier = Modifier.then(if (scrubbing) Modifier.clickable(onClick = onReset) else Modifier),
                )
                Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(numberW)) {
                        Text("${m.temp.roundToInt()}°", fontSize = numberSp.sp, lineHeight = (numberSp * 1.05f).sp, fontWeight = FontWeight.Light, color = ink, maxLines = 1, softWrap = false)
                        Text(m.condition.label, fontSize = 18.sp, color = ink, maxLines = 1)
                        Text(
                            when {
                                m.precip != null && m.precip >= 10 -> "Lluvia ${m.precip}%"
                                today != null -> "Máx ${today.tempMax.roundToInt()}° · Mín ${today.tempMin.roundToInt()}°"
                                else -> " "
                            },
                            fontSize = 12.sp, color = soft, maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Crossfade(m.condition to m.isDay, animationSpec = tween(350), label = "scene", modifier = Modifier.size(128.dp)) { (c, day) ->
                        FlatScene(c, day, { clock.value }, Modifier.fillMaxSize())
                    }
                }
                if (live && !scrubbing && !hinted) {
                    Text("↕ Deslizá sobre las franjas", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = soft, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Escenas planas animadas: una por tipo de clima. Se dibujan en un cuadrado y toman el tiempo en segundos.
// ---------------------------------------------------------------------------------------------

private val SUN = Color(0xFFFFD36B)
private val SUN_EDGE = Color(0xFF9A5A2E)
private val MOON = Color(0xFFFFEBBE)
private val CLOUD = Color(0xFFFFF1D6)
private val CLOUD_BACK = Color(0xFFE9CFA6)
private val CLOUD_STORM = Color(0xFFC8AA86)
private val DROP = Color(0xFFBFE0F2)
private val SNOW = Color(0xFFFFFAF0)
private val BOLT = Color(0xFFFFE066)
private val SHADOW = Color(0x2E2B1810)

private fun fract(x: Float) = x - floor(x)

@Composable
fun FlatScene(condition: Condition, isDay: Boolean, time: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val t = time()
        val s = size.minDimension
        val o = Offset((size.width - s) / 2f, (size.height - s) / 2f)
        translate(o.x, o.y) {
            when (condition) {
                Condition.Clear -> if (isDay) sun(Offset(.5f * s, .5f * s), .2f * s, t) else night(s, t, Offset(.5f * s, .5f * s), .22f * s)
                Condition.PartlyCloudy -> {
                    if (isDay) sun(Offset(.37f * s, .38f * s), .15f * s, t) else night(s, t, Offset(.37f * s, .38f * s), .16f * s)
                    cloud(Offset(.56f * s + sin(t * .6f) * .03f * s, .6f * s), .64f * s, CLOUD)
                }
                Condition.Cloudy -> {
                    cloud(Offset(.36f * s - sin(t * .5f) * .03f * s, .38f * s), .46f * s, CLOUD_BACK)
                    cloud(Offset(.58f * s + sin(t * .6f) * .035f * s, .58f * s), .68f * s, CLOUD)
                }
                Condition.Drizzle -> {
                    if (isDay) sun(Offset(.34f * s, .3f * s), .13f * s, t) else night(s, t, Offset(.34f * s, .3f * s), .14f * s)
                    cloud(Offset(.56f * s + sin(t * .6f) * .02f * s, .42f * s), .62f * s, CLOUD)
                    drops(s, t, count = 5, speed = .45f, y0 = .62f, y1 = .95f, r = .026f)
                }
                Condition.Rain -> {
                    cloud(Offset(.5f * s + sin(t * .6f) * .02f * s, .34f * s), .68f * s, CLOUD)
                    drops(s, t, count = 9, speed = .8f, y0 = .56f, y1 = .97f, r = .036f)
                }
                Condition.Thunderstorm -> {
                    cloud(Offset(.5f * s + sin(t * .7f) * .02f * s, .34f * s), .7f * s, CLOUD_STORM)
                    drops(s, t, count = 6, speed = 1.1f, y0 = .58f, y1 = .97f, r = .03f)
                    val ph = fract(t * .33f)
                    val flash = if (ph < .06f || (ph in .12f..0.17f)) 1f else .22f
                    bolt(s, flash)
                }
                Condition.Snow -> {
                    cloud(Offset(.5f * s, .34f * s), .68f * s, CLOUD)
                    flakes(s, t)
                }
                Condition.Fog -> {
                    cloud(Offset(.5f * s, .3f * s), .56f * s, CLOUD)
                    for (i in 0 until 4) {
                        val y = (.56f + i * .12f) * s
                        val dx = sin(t * (.35f + i * .08f) + i * 1.7f) * .09f * s
                        val w = (.6f - (i % 2) * .12f) * s
                        drawLine(CLOUD.copy(alpha = .85f), Offset(.5f * s - w / 2 + dx, y), Offset(.5f * s + w / 2 + dx, y), strokeWidth = .055f * s, cap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}

/** Sol radiante: halo que respira y rayos que giran y laten, cada uno a su ritmo. */
private fun DrawScope.sun(c: Offset, r: Float, t: Float) {
    val pulse = .5f + .5f * sin(t * 1.6f)
    drawCircle(SUN.copy(alpha = .14f + .08f * pulse), r * (1.6f + .14f * pulse), c)
    drawCircle(SUN.copy(alpha = .22f), r * 1.28f, c)
    val n = 12
    for (i in 0 until n) {
        val a = t * .25f + i * (2f * PI.toFloat() / n)
        val start = r * 1.42f
        val len = r * (.3f + .17f * sin(t * 2.4f + i * .9f))
        val dir = Offset(cos(a), sin(a))
        drawLine(SUN, c + dir * start, c + dir * (start + len), strokeWidth = r * .15f, cap = StrokeCap.Round)
    }
    drawCircle(SUN, r, c)
    drawCircle(SUN_EDGE.copy(alpha = .35f), r, c, style = Stroke(r * .07f))
}

private fun DrawScope.night(s: Float, t: Float, c: Offset, r: Float) {
    val stars = listOf(.16f to .2f, .82f to .16f, .72f to .52f, .24f to .74f, .6f to .86f, .9f to .8f)
    stars.forEachIndexed { i, (x, y) ->
        val a = .35f + .65f * (.5f + .5f * sin(t * 1.8f + i * 1.3f))
        drawCircle(MOON.copy(alpha = a), s * (.012f + .006f * a), Offset(x * s, y * s))
    }
    val glow = .5f + .5f * sin(t * .9f)
    drawCircle(MOON.copy(alpha = .1f + .06f * glow), r * (1.7f + .1f * glow), c)
    val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
    val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c + Offset(r * .5f, -r * .28f), r * .86f)) }
    drawPath(Path.combine(PathOperation.Difference, disc, bite), MOON)
}

private fun DrawScope.cloud(c: Offset, w: Float, fill: Color) {
    val h = w * .55f
    fun shape(dy: Float, col: Color) {
        val base = c.y + h * .5f + dy
        drawRoundRect(col, Offset(c.x - w / 2, base - h * .42f), Size(w, h * .42f), CornerRadius(h * .21f))
        drawCircle(col, h * .34f, Offset(c.x - w * .22f, base - h * .42f))
        drawCircle(col, h * .46f, Offset(c.x + w * .04f, base - h * .62f))
        drawCircle(col, h * .30f, Offset(c.x + w * .28f, base - h * .42f))
    }
    shape(h * .07f, SHADOW)
    shape(0f, fill)
}

private fun DrawScope.drop(x: Float, y: Float, r: Float, a: Float) {
    val col = DROP.copy(alpha = a)
    val p = Path().apply {
        moveTo(x, y - r * 2.1f)
        lineTo(x - r * .92f, y - r * .35f)
        lineTo(x + r * .92f, y - r * .35f)
        close()
    }
    drawPath(p, col)
    drawCircle(col, r, Offset(x, y))
}

private fun DrawScope.drops(s: Float, t: Float, count: Int, speed: Float, y0: Float, y1: Float, r: Float) {
    for (i in 0 until count) {
        val col = i % 3
        val x = (.5f + (col - 1) * .17f + ((i / 3) % 2) * .05f) * s
        val ph = fract(t * speed + i * .237f)
        val y = (y0 + (y1 - y0) * ph) * s
        drop(x, y, r * s, (1f - ph).coerceIn(0f, 1f).let { minOf(1f, it * 3f) })
    }
}

private fun DrawScope.flakes(s: Float, t: Float) {
    for (i in 0 until 6) {
        val ph = fract(t * .16f + i * .19f)
        val x = (.22f + (i % 3) * .28f + (i / 3) * .08f) * s + sin(t * 1.1f + i) * .035f * s
        val y = (.56f + .4f * ph) * s
        val a = minOf(1f, (1f - ph) * 3f)
        val rot = t * .8f + i
        val len = .045f * s
        for (k in 0 until 3) {
            val ang = rot + k * PI.toFloat() / 3f
            val d = Offset(cos(ang), sin(ang)) * len
            drawLine(SHADOW.copy(alpha = .16f * a), Offset(x, y) - d + Offset(0f, 2f), Offset(x, y) + d + Offset(0f, 2f), strokeWidth = .02f * s, cap = StrokeCap.Round)
            drawLine(SNOW.copy(alpha = a), Offset(x, y) - d, Offset(x, y) + d, strokeWidth = .018f * s, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.bolt(s: Float, alpha: Float) {
    val p = Path().apply {
        moveTo(.54f * s, .52f * s)
        lineTo(.42f * s, .74f * s)
        lineTo(.5f * s, .74f * s)
        lineTo(.44f * s, .96f * s)
        lineTo(.62f * s, .68f * s)
        lineTo(.53f * s, .68f * s)
        lineTo(.6f * s, .52f * s)
        close()
    }
    drawPath(p, BOLT.copy(alpha = alpha))
}
