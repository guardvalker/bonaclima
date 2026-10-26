package com.guardvalker.bonaclima.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guardvalker.bonaclima.domain.DailyPoint
import com.guardvalker.bonaclima.domain.Forecast
import com.guardvalker.bonaclima.domain.Place
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val AR = Locale.forLanguageTag("es-AR")
private val HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm")

private const val MAIN = "main"
private const val SEARCH = "search"
private const val SETTINGS = "settings"
private const val CHANGELOG_SCREEN = "changelog"

@Composable
fun WeatherApp(vm: WeatherViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val baseStyle = styleById(state.styleId)
    val sysDark = androidx.compose.foundation.isSystemInDarkTheme()
    val gScheme = if (baseStyle.google) googleScheme(sysDark) else null
    val style = if (gScheme != null) googleStyle(baseStyle, gScheme, sysDark) else baseStyle
    var screen by rememberSaveable { mutableStateOf(MAIN) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshIfStale(); vm.checkForAppUpdate() }
    BackHandler(enabled = screen != MAIN) { screen = if (screen == CHANGELOG_SCREEN) SETTINGS else MAIN }

    // Íconos de las barras del sistema según el estilo (oscuros sobre papel claro, claros sobre papel oscuro).
    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(style.dark) {
        val bars = if (style.dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        activity.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        onDispose {}
    }

    val scheme = gScheme ?: if (style.dark) {
        darkColorScheme(
            primary = style.accent, onPrimary = style.paper, background = style.paper, onBackground = style.ink,
            surface = style.card, onSurface = style.ink, surfaceContainerHigh = style.card,
            surfaceVariant = style.card, onSurfaceVariant = style.inkSoft, outline = style.line,
        )
    } else {
        lightColorScheme(
            primary = style.accent, onPrimary = style.paper, background = style.paper, onBackground = style.ink,
            surface = style.card, onSurface = style.ink, surfaceContainerHigh = style.card,
            surfaceVariant = style.card, onSurfaceVariant = style.inkSoft, outline = style.line,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalStyle provides style,
            LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = if (style.google) FontFamily.SansSerif else FontFamily.Serif, color = style.ink)),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (style.google) Box(Modifier.fillMaxSize().background(style.paper)) else PaperBackground(style)
                Crossfade(screen, animationSpec = tween(220), label = "screen") { current ->
                    when (current) {
                        SEARCH -> SearchPanel(
                            results = state.searchResults,
                            onQuery = vm::search,
                            onPick = { vm.select(it); screen = MAIN },
                            onMyLocation = { vm.useMyLocation(); screen = MAIN },
                            onClose = { screen = MAIN },
                        )
                        SETTINGS -> SettingsScreen(
                            state, vm.version, onBack = { screen = MAIN }, onStyle = vm::setStyle,
                            onCheckUpdate = { vm.checkForAppUpdate(manual = true) }, onChangelog = { screen = CHANGELOG_SCREEN },
                        )
                        CHANGELOG_SCREEN -> ChangelogScreen(vm.version, onBack = { screen = SETTINGS })
                        else -> MainContent(state, vm.version, style, onSearch = { screen = SEARCH }, onSettings = { screen = SETTINGS }, onRefresh = vm::refresh)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(state: UiState, version: String, style: BonaStyle, onSearch: () -> Unit, onSettings: () -> Unit, onRefresh: () -> Unit) {
    if (style.google) {
        GoogleMain(state, version, style, onSearch, onSettings, onRefresh)
        return
    }
    if (style.bands) {
        BandsMain(state, version, style, onSearch, onSettings, onRefresh)
        return
    }
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clip(PaperShape).clickable(onClick = onSearch).padding(vertical = 8.dp),
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
                Text(
                    state.error ?: "Cargando…", color = style.inkSoft, fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp,
                )
            } else {
                val sky = skyFor(f, style)
                Reveal(0) { Hero(f, sky, style) }
                if (state.error != null) Text(state.error, color = style.accent, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Reveal(1) { HourlyCard(f, style) }
                Reveal(2) { DailyCard(f, style) }
                Reveal(3) { DetailsGrid(f, style) }
                Text(
                    buildString {
                        append("BonaClima v$version · Datos: Open-Meteo · actualizado ${updatedAgo(f)}")
                        state.updateStatus?.takeIf { it.startsWith("Actualizando") }?.let { append("\n$it") }
                    },
                    color = style.inkSoft, fontSize = 11.sp, fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Entrada escalonada: cada bloque aparece con fade + deslizamiento, con un pequeño retraso por índice. */
@Composable
private fun Reveal(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80L * index); shown = true }
    AnimatedVisibility(shown, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 }) { content() }
}

/** Viñeta enmarcada con el cielo animado y la temperatura actual. */
@Composable
private fun Hero(f: Forecast, sky: SkyState, style: BonaStyle) {
    val ink = heroInk(sky)
    val c = f.current
    val today = f.daily.firstOrNull()
    Box(Modifier.fillMaxWidth().height(340.dp).clip(PaperShape).border(1.5.dp, style.ink.copy(alpha = 0.75f), PaperShape)) {
        WeatherBackground(sky, style, ink)
        Box(Modifier.fillMaxSize().padding(6.dp).border(0.75.dp, ink.copy(alpha = 0.5f), PaperShape))
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            WeatherGlyph(c.condition, c.isDay, ink, 60.dp)
            Text("${c.temperature.roundToInt()}°", fontSize = 96.sp, fontWeight = FontWeight.Light, color = ink)
            Text(c.condition.label, fontSize = 22.sp, fontStyle = FontStyle.Italic, color = ink)
            Spacer(Modifier.height(6.dp))
            Text(
                buildString {
                    append("Sensación ${c.apparentTemperature.roundToInt()}°")
                    if (today != null) append("  ·  Máx ${today.tempMax.roundToInt()}°  Mín ${today.tempMin.roundToInt()}°")
                },
                fontSize = 14.sp, color = ink.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
internal fun HourlyCard(f: Forecast, style: BonaStyle) {
    val now = f.current.time.withMinute(0).withSecond(0).withNano(0)
    val hours = f.hourly.filter { !it.time.isBefore(now) }.take(25)
    PaperCard("Próximas horas") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            items(hours) { h ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (h.time == now) "Ahora" else HOUR_FMT.format(h.time), fontSize = 13.sp, color = style.inkSoft)
                    Spacer(Modifier.height(8.dp))
                    WeatherGlyph(h.condition, h.isDay, style.ink, 28.dp)
                    val p = h.precipProbability ?: 0
                    Text(if (p >= 10) "$p%" else " ", fontSize = 11.sp, color = style.water)
                    Text("${h.temperature.roundToInt()}°", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = style.ink)
                }
            }
        }
    }
}

@Composable
internal fun DailyCard(f: Forecast, style: BonaStyle) {
    val days = f.daily.take(8)
    val lo = days.minOf { it.tempMin }
    val hi = days.maxOf { it.tempMax }
    PaperCard("Próximos días") {
        Column {
            days.forEachIndexed { i, d ->
                if (i > 0) Rule()
                DayRow(d, i == 0, lo, hi, style)
            }
        }
    }
}

@Composable
private fun DayRow(d: DailyPoint, isToday: Boolean, lo: Double, hi: Double, style: BonaStyle) {
    val span = (hi - lo).coerceAtLeast(1.0)
    val start = ((d.tempMin - lo) / span).toFloat().coerceIn(0f, 1f)
    val width = ((d.tempMax - d.tempMin) / span).toFloat().coerceIn(0.04f, 1f - start)
    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (isToday) "Hoy" else d.date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, AR).replaceFirstChar { it.uppercase() }.trimEnd('.'),
            modifier = Modifier.width(46.dp), fontSize = 16.sp, color = style.ink,
        )
        WeatherGlyph(d.condition, true, style.ink, 26.dp)
        Spacer(Modifier.width(8.dp))
        val p = d.precipProbability ?: 0
        Text(if (p >= 10) "$p%" else "", modifier = Modifier.width(38.dp), fontSize = 12.sp, color = style.water)
        Text("${d.tempMin.roundToInt()}°", modifier = Modifier.width(34.dp), textAlign = TextAlign.End, fontSize = 15.sp, color = style.inkSoft)
        Row(Modifier.weight(1f).padding(horizontal = 8.dp).height(4.dp).background(style.line.copy(alpha = 0.45f))) {
            if (start > 0f) Spacer(Modifier.weight(start))
            Box(Modifier.weight(width).height(4.dp).background(style.accent))
            val rest = 1f - start - width
            if (rest > 0.001f) Spacer(Modifier.weight(rest))
        }
        Text("${d.tempMax.roundToInt()}°", modifier = Modifier.width(34.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = style.ink)
    }
}

@Composable
internal fun DetailsGrid(f: Forecast, style: BonaStyle) {
    val c = f.current
    val today = f.daily.firstOrNull()
    val items = buildList {
        c.humidity?.let { add(Triple(Glyph.Drop, "Humedad", "$it%")) }
        c.windSpeedKmh?.let { add(Triple(Glyph.Wind, "Viento", "${it.roundToInt()} km/h ${c.windDirectionDeg?.let(::compass).orEmpty()}")) }
        c.pressureHpa?.let { add(Triple(Glyph.Gauge, "Presión", "${it.roundToInt()} hPa")) }
        c.uvIndex?.let { add(Triple(Glyph.Sun, "Índice UV", uvLabel(it))) }
        today?.sunrise?.let { add(Triple(Glyph.Sunrise, "Amanecer", HOUR_FMT.format(it))) }
        today?.sunset?.let { add(Triple(Glyph.Sunset, "Atardecer", HOUR_FMT.format(it))) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (glyph, k, v) ->
                    Column(Modifier.weight(1f).clip(PaperShape).background(style.card).border(1.dp, style.line, PaperShape).padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UiGlyph(glyph, style.accent, 20.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(k.uppercase(AR), fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, color = style.accent)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(v, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = style.ink)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SearchPanel(results: List<Place>, onQuery: (String) -> Unit, onPick: (Place) -> Unit, onMyLocation: () -> Unit, onClose: () -> Unit) {
    val st = LocalStyle.current
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(350)
        if (query.length >= 2) onQuery(query)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) onMyLocation() }

    Column(Modifier.fillMaxSize()) {
        TopBar("Elegir ciudad", onClose)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Buscar ciudad", color = st.inkSoft) },
                leadingIcon = { UiGlyph(Glyph.Search, st.accent, 22.dp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = PaperShape,
            )
            PaperCard(null, Modifier.clickable { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                PaperRow(Glyph.Pin, "Usar mi ubicación")
            }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                results.forEachIndexed { i, p ->
                    if (i > 0) Rule()
                    Column(Modifier.fillMaxWidth().clickable { onPick(p) }.padding(vertical = 12.dp)) {
                        Text(p.name, fontSize = 18.sp, color = st.ink)
                        p.region?.let { Text(it, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = st.inkSoft) }
                    }
                }
            }
        }
    }
}

internal fun compass(deg: Int): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    return dirs[((deg + 22.5) / 45).toInt() % 8]
}

internal fun uvLabel(uv: Double) = when {
    uv < 3 -> "Bajo (${uv.roundToInt()})"
    uv < 6 -> "Moderado (${uv.roundToInt()})"
    uv < 8 -> "Alto (${uv.roundToInt()})"
    uv < 11 -> "Muy alto (${uv.roundToInt()})"
    else -> "Extremo (${uv.roundToInt()})"
}

internal fun updatedAgo(f: Forecast): String {
    val min = ((System.currentTimeMillis() - f.fetchedAtMillis) / 60_000).toInt()
    return when {
        min < 1 -> "recién"
        min < 60 -> "hace $min min"
        else -> "hace ${min / 60} h"
    }
}
