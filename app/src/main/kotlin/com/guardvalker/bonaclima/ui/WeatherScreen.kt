package com.guardvalker.bonaclima.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val AR = Locale.forLanguageTag("es-AR")
private val CARD = Color.Black.copy(alpha = 0.2f)
private val HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun WeatherApp(vm: WeatherViewModel) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val state by vm.state.collectAsStateWithLifecycle()
        var searching by rememberSaveable { mutableStateOf(false) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshIfStale(); vm.checkForAppUpdate() }

        Box(Modifier.fillMaxSize()) {
            WeatherBackground(skyFor(state.forecast))
            if (searching) {
                SearchPanel(
                    results = state.searchResults,
                    onQuery = vm::search,
                    onPick = { vm.select(it); searching = false },
                    onMyLocation = { vm.useMyLocation(); searching = false },
                    onClose = { searching = false },
                )
            } else {
                MainContent(state, vm.version, onSearch = { searching = true }, onRefresh = vm::refresh)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(state: UiState, version: String, onSearch: () -> Unit, onRefresh: () -> Unit) {
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onSearch).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📍", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(state.place.name, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("Cambiar", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
            val f = state.forecast
            if (f == null) {
                Spacer(Modifier.height(120.dp))
                Text(
                    state.error ?: "Cargando…", color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp,
                )
            } else {
                Reveal(0) { Hero(f) }
                if (state.error != null) Text(state.error, color = Color(0xFFFFD6A0), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Reveal(1) { HourlyCard(f) }
                Reveal(2) { DailyCard(f) }
                Reveal(3) { DetailsGrid(f) }
                Text(
                    "BonaClima v$version · Datos: Open-Meteo · actualizado ${updatedAgo(f)}", color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
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

@Composable
private fun Hero(f: Forecast) {
    val c = f.current
    val today = f.daily.firstOrNull()
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${c.temperature.roundToInt()}°", fontSize = 104.sp, fontWeight = FontWeight.Thin, color = Color.White)
        Text("${c.condition.emoji(c.isDay)}  ${c.condition.label}", fontSize = 20.sp, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(
            buildString {
                append("Sensación ${c.apparentTemperature.roundToInt()}°")
                if (today != null) append("  ·  Máx ${today.tempMax.roundToInt()}°  Mín ${today.tempMin.roundToInt()}°")
            },
            fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CARD).padding(16.dp)) {
        Text(title.uppercase(AR), fontSize = 12.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.65f))
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun HourlyCard(f: Forecast) {
    val now = f.current.time.withMinute(0).withSecond(0).withNano(0)
    val hours = f.hourly.filter { !it.time.isBefore(now) }.take(25)
    Card("Próximas horas") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            items(hours) { h ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (h.time == now) "Ahora" else HOUR_FMT.format(h.time), fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Text(h.condition.emoji(h.isDay), fontSize = 24.sp)
                    val p = h.precipProbability ?: 0
                    Text(if (p >= 10) "$p%" else " ", fontSize = 11.sp, color = Color(0xFF9FD0FF))
                    Text("${h.temperature.roundToInt()}°", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DailyCard(f: Forecast) {
    val days = f.daily.take(8)
    val lo = days.minOf { it.tempMin }
    val hi = days.maxOf { it.tempMax }
    Card("Próximos días") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            days.forEachIndexed { i, d -> DayRow(d, i == 0, lo, hi) }
        }
    }
}

@Composable
private fun DayRow(d: DailyPoint, isToday: Boolean, lo: Double, hi: Double) {
    val span = (hi - lo).coerceAtLeast(1.0)
    val start = ((d.tempMin - lo) / span).toFloat().coerceIn(0f, 1f)
    val width = ((d.tempMax - d.tempMin) / span).toFloat().coerceIn(0.04f, 1f - start)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (isToday) "Hoy" else d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, AR).replaceFirstChar { it.uppercase() }.trimEnd('.'),
            modifier = Modifier.width(46.dp), fontSize = 15.sp, color = Color.White,
        )
        Text(d.condition.dayEmoji, fontSize = 20.sp, modifier = Modifier.width(34.dp))
        val p = d.precipProbability ?: 0
        Text(if (p >= 10) "$p%" else "", modifier = Modifier.width(38.dp), fontSize = 12.sp, color = Color(0xFF9FD0FF))
        Text("${d.tempMin.roundToInt()}°", modifier = Modifier.width(34.dp), textAlign = TextAlign.End, fontSize = 15.sp, color = Color.White.copy(alpha = 0.65f))
        Row(Modifier.weight(1f).padding(horizontal = 8.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.15f))) {
            if (start > 0f) Spacer(Modifier.weight(start))
            Box(
                Modifier.weight(width).height(5.dp).clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF7CC4FF), Color(0xFFFFC857)))),
            )
            val rest = 1f - start - width
            if (rest > 0.001f) Spacer(Modifier.weight(rest))
        }
        Text("${d.tempMax.roundToInt()}°", modifier = Modifier.width(34.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@Composable
private fun DetailsGrid(f: Forecast) {
    val c = f.current
    val today = f.daily.firstOrNull()
    val items = buildList {
        c.humidity?.let { add("Humedad" to "$it%") }
        c.windSpeedKmh?.let { add("Viento" to "${it.roundToInt()} km/h ${c.windDirectionDeg?.let(::compass).orEmpty()}") }
        c.pressureHpa?.let { add("Presión" to "${it.roundToInt()} hPa") }
        c.uvIndex?.let { add("Índice UV" to uvLabel(it)) }
        today?.sunrise?.let { add("Amanecer" to HOUR_FMT.format(it)) }
        today?.sunset?.let { add("Atardecer" to HOUR_FMT.format(it)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (k, v) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(CARD).padding(16.dp)) {
                        Text(k.uppercase(AR), fontSize = 11.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.65f))
                        Spacer(Modifier.height(6.dp))
                        Text(v, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SearchPanel(results: List<Place>, onQuery: (String) -> Unit, onPick: (Place) -> Unit, onMyLocation: () -> Unit, onClose: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(350)
        if (query.length >= 2) onQuery(query)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) onMyLocation() }

    Column(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true,
                placeholder = { Text("Buscar ciudad") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            )
            TextButton(onClick = onClose) { Text("Cerrar") }
        }
        TextButton(onClick = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) { Text("📍 Usar mi ubicación") }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            results.forEach { p ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onPick(p) }.padding(12.dp)) {
                    Text(p.name, fontSize = 17.sp, color = Color.White)
                    p.region?.let { Text(it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)) }
                }
            }
        }
    }
}

private fun compass(deg: Int): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    return dirs[((deg + 22.5) / 45).toInt() % 8]
}

private fun uvLabel(uv: Double) = when {
    uv < 3 -> "Bajo (${uv.roundToInt()})"
    uv < 6 -> "Moderado (${uv.roundToInt()})"
    uv < 8 -> "Alto (${uv.roundToInt()})"
    uv < 11 -> "Muy alto (${uv.roundToInt()})"
    else -> "Extremo (${uv.roundToInt()})"
}

private fun updatedAgo(f: Forecast): String {
    val min = ((System.currentTimeMillis() - f.fetchedAtMillis) / 60_000).toInt()
    return when {
        min < 1 -> "recién"
        min < 60 -> "hace $min min"
        else -> "hace ${min / 60} h"
    }
}
