package com.guardvalker.bonaclima.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    state: UiState,
    version: String,
    onBack: () -> Unit,
    onStyle: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onChangelog: () -> Unit,
) {
    val st = LocalStyle.current
    Column(Modifier.fillMaxSize()) {
        TopBar("Ajustes", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PaperCard("Estilo") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    STYLES.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { s -> StyleTile(s, s.id == state.styleId, Modifier.weight(1f)) { onStyle(s.id) } }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            PaperCard("Acerca de") {
                Column {
                    PaperRow(Glyph.Info, "Versión", value = "v$version", chevron = true, onClick = onChangelog)
                    Rule()
                    PaperRow(
                        Glyph.Refresh, "Buscar actualizaciones",
                        subtitle = if (state.checkingUpdate) "Buscando…" else state.updateStatus ?: "Toca para comprobar",
                        onClick = onCheckUpdate,
                    )
                    Rule()
                    PaperRow(Glyph.Book, "Historial de cambios", chevron = true, onClick = onChangelog)
                    Rule()
                    PaperRow(Glyph.Sun, "Fuente de datos", value = "Open-Meteo")
                }
            }
            Text(
                "BonaClima v$version", fontSize = 12.sp, fontStyle = FontStyle.Italic, color = st.inkSoft,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** Muestra miniatura del estilo: hoja, tinta y acento reales, para ver cómo queda antes de elegirlo. */
@Composable
private fun StyleTile(s: BonaStyle, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val now = LocalStyle.current
    val shape = PaperShape
    Column(
        modifier.clip(shape).background(s.paper)
            .border(if (selected) 2.dp else 1.dp, if (selected) now.accent else s.line, shape)
            .clickable(onClick = onClick).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Aa", fontSize = 26.sp, color = s.ink, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            if (selected) UiGlyph(Glyph.Check, now.accent, 20.dp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(s.accent))
        Spacer(Modifier.height(6.dp))
        Text(s.name, fontSize = 15.sp, color = s.ink, fontWeight = FontWeight.Bold)
        Text(s.tagline, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = s.inkSoft, maxLines = 1)
    }
}

@Composable
fun ChangelogScreen(version: String, onBack: () -> Unit) {
    val st = LocalStyle.current
    Column(Modifier.fillMaxSize()) {
        TopBar("Historial de cambios", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CHANGELOG.forEach { e ->
                PaperCard(null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("v${e.version}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = st.ink)
                        Spacer(Modifier.weight(1f))
                        Text(if (e.version == version) "instalada · ${e.date}" else e.date, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = st.inkSoft)
                    }
                    Spacer(Modifier.height(8.dp))
                    Rule()
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        e.items.forEach { Text("•  $it", fontSize = 14.sp, color = st.ink, lineHeight = 20.sp) }
                    }
                }
            }
        }
    }
}
