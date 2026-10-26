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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.Job
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
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

private enum class Period(val label: String, val range: String, val color: Color, val repHour: Float) {
    Morning("Mañana", "6–12 h", C_MORNING, 9f),
    Afternoon("Tarde", "12–18 h", C_AFTERNOON, 15f),
    Dusk("Tardecita", "18–21 h", C_DUSK, 19.5f),
    Night("Noche", "21–6 h", C_NIGHT, 23f),
}

private fun periodIndex(hour: Float) = when {
    hour < 6f -> 3
    hour < 12f -> 0
    hour < 18f -> 1
    hour < 21f -> 2
    else -> 3
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

private val SCENE = 92.dp

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
                    val currentIdx = periodIndex(nowHour)
                    val scope = rememberCoroutineScope()
                    var selected by rememberSaveable { mutableIntStateOf(currentIdx) }
                    // Posición en "índices de franja" (0 = Mañana … 3 = Noche): una sola animación, sin estados intermedios por hora.
                    val pos = remember { Animatable(selected.toFloat()) }
                    var drag by remember { mutableFloatStateOf(0f) }
                    val job = remember { arrayOfNulls<Job>(1) }
                    val p = (pos.value + drag).coerceIn(0f, 3f)
                    val dominant = p.roundToInt().coerceIn(0, 3)

                    fun hourOf(idx: Int) = if (idx == currentIdx) nowHour else Period.entries[idx].repHour
                    fun momentOf(idx: Int) =
                        if (idx == currentIdx) Moment(f.current.temperature, f.current.condition, f.current.isDay, null)
                        else momentAt(f, Period.entries[idx].repHour)
                    /** Anima hasta la franja [idx] desde donde esté (incluido lo que el dedo haya arrastrado). */
                    fun goTo(idx: Int) {
                        val from = (pos.value + drag).coerceIn(0f, 3f)
                        drag = 0f
                        selected = idx
                        job[0]?.cancel()
                        job[0] = scope.launch {
                            pos.snapTo(from)
                            pos.animateTo(idx.toFloat(), tween(300, easing = FastOutSlowInEasing))
                        }
                    }

                    val haptic = LocalHapticFeedback.current
                    var lastSelected by remember { mutableIntStateOf(selected) }
                    LaunchedEffect(selected) {
                        if (selected != lastSelected) haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        lastSelected = selected
                    }

                    val thresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
                    val panelH = screenH * 0.46f

                    // Un gesto sobre el panel mueve solo hasta la franja contigua: acompaña un poco al dedo y salta solo al pasar el umbral.
                    Row(
                        Modifier.fillMaxWidth().height(panelH).pointerInput(currentIdx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                val slop = viewConfiguration.touchSlop
                                var total = 0f
                                var acc = 0f
                                var dragging = false
                                var fired = false
                                val base = selected
                                job[0]?.cancel()
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                                    val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!ch.pressed) break
                                    val dy = ch.position.y - ch.previousPosition.y
                                    if (!dragging) {
                                        total += dy
                                        if (kotlin.math.abs(total) <= slop) continue
                                        dragging = true
                                        acc = total
                                    } else acc += dy
                                    // Se consume en la pasada inicial: la pantalla no scrollea mientras se arrastra sobre el panel.
                                    ch.consume()
                                    if (fired) continue
                                    // Dedo hacia abajo = franja anterior, hacia arriba = la siguiente.
                                    val target = base + if (acc > 0) -1 else 1
                                    if (target !in 0..3) { drag = 0f; continue }
                                    val k = (kotlin.math.abs(acc) / thresholdPx).coerceIn(0f, 1f)
                                    if (k >= 1f) {
                                        fired = true
                                        goTo(target)
                                    } else {
                                        drag = (target - base) * k * 0.3f
                                    }
                                }
                                if (!fired) {
                                    if (dragging) goTo(base) else job[0] = null
                                }
                            }
                        },
                    ) {
                        val i0 = p.toInt().coerceIn(0, 3)
                        val i1 = (i0 + 1).coerceAtMost(3)
                        val fr = p - i0
                        // Barrido vertical: la franja que entra empuja una línea de color sobre la otra (texto incluido, con su propio contraste).
                        Box(Modifier.weight(1f).fillMaxSize()) {
                            Panel(i0, momentOf(i0), f, i0 != currentIdx, hourOf(i0), clock, Modifier.fillMaxSize().clipBand(0f, 1f - fr)) { goTo(currentIdx) }
                            if (fr > 0.001f && i1 != i0) {
                                Panel(i1, momentOf(i1), f, i1 != currentIdx, hourOf(i1), clock, Modifier.fillMaxSize().clipBand(1f - fr, 1f)) { goTo(currentIdx) }
                            }
                        }
                        Selector(f, dominant, Modifier.width(62.dp).fillMaxSize()) { goTo(it) }
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

private fun Modifier.clipBand(from: Float, to: Float) = drawWithContent {
    clipRect(0f, size.height * from, size.width, size.height * to) { this@drawWithContent.drawContent() }
}

/** Panel único: número y escena siempre en el mismo lugar; solo cambian el color, los valores y el nombre del momento. */
@Composable
private fun Panel(
    idx: Int, m: Moment, f: Forecast, scrubbing: Boolean, hour: Float, clock: State<Float>,
    modifier: Modifier, onReset: () -> Unit,
) {
    val p = Period.entries[idx]
    val color = p.color
    val ink = inkOn(color)
    val soft = ink.copy(alpha = 0.78f)
    val today = f.daily.firstOrNull()
    BoxWithConstraints(modifier.background(color)) {
        val numberSp = (maxWidth.value / 1.6f).coerceAtMost(maxHeight.value * 0.5f).coerceAtLeast(64f)
        Text(
            (if (scrubbing) "${p.label} · ${hourLabel(hour)} · ↺ AHORA" else "${p.label} · AHORA").uppercase(AR),
            fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold, color = soft, maxLines = 1, softWrap = false,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp).then(if (scrubbing) Modifier.clickable(onClick = onReset) else Modifier),
        )
        FlatScene(m.condition, m.isDay, { clock.value }, Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 12.dp).size(SCENE))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 16.dp)) {
            Text("${m.temp.roundToInt()}°", fontSize = numberSp.sp, lineHeight = (numberSp * 1.02f).sp, fontWeight = FontWeight.Light, color = ink, maxLines = 1, softWrap = false)
            Text(m.condition.label, fontSize = 20.sp, color = ink, maxLines = 1)
            Text(
                when {
                    m.precip != null && m.precip >= 10 -> "Lluvia ${m.precip}%"
                    today != null -> "Máx ${today.tempMax.roundToInt()}° · Mín ${today.tempMin.roundToInt()}°"
                    else -> " "
                },
                fontSize = 12.sp, color = soft, maxLines = 1,
            )
        }
    }
}

/** Selector de color al costado: una muestra por momento del día, con su temperatura chica. Tocar una la elige. */
@Composable
private fun Selector(f: Forecast, active: Int, modifier: Modifier, onPick: (Int) -> Unit) {
    Column(modifier) {
        Period.entries.forEachIndexed { i, p ->
            val ink = inkOn(p.color)
            val on = i == active
            Column(
                Modifier.weight(1f).fillMaxWidth().background(p.color).clickable { onPick(i) }.padding(horizontal = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                val a = if (on) 1f else 0.7f
                Text(
                    p.label.uppercase(AR), fontSize = if (p.label.length > 7) 8.sp else 9.sp, letterSpacing = 0.3.sp, fontWeight = FontWeight.Bold,
                    color = ink.copy(alpha = a), maxLines = 1, softWrap = false,
                )
                Text(
                    "${momentAt(f, p.repHour).temp.roundToInt()}°", fontSize = if (on) 17.sp else 14.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, color = ink.copy(alpha = a),
                )
                Text(p.range, fontSize = 9.sp, color = ink.copy(alpha = a * 0.85f), maxLines = 1, softWrap = false)
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
