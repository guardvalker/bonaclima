package com.guardvalker.bonaclima.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

val PaperShape = RoundedCornerShape(3.dp)

/** Hoja de fondo: color de papel, fibras sueltas, borde oscurecido y, en "Cuaderno", renglones con margen. */
@Composable
fun PaperBackground(style: BonaStyle, modifier: Modifier = Modifier) {
    val specks = remember { List(700) { Triple(Random(7 + it).nextFloat(), Random(900 + it).nextFloat(), Random(31 + it).nextFloat()) } }
    Canvas(modifier.fillMaxSize()) {
        drawRect(style.paper)
        if (style.ruled) {
            val gap = 30.dp.toPx()
            var y = gap * 2
            while (y < size.height) {
                drawLine(style.line.copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += gap
            }
            drawLine(style.accent.copy(alpha = 0.45f), Offset(34.dp.toPx(), 0f), Offset(34.dp.toPx(), size.height), strokeWidth = 1.5f)
        }
        for ((x, y, s) in specks) {
            drawCircle(style.ink.copy(alpha = 0.035f + 0.05f * s), (0.4f + s * 1.1f), Offset(x * size.width, y * size.height))
        }
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, style.ink.copy(alpha = if (style.dark) 0.28f else 0.12f)),
                center = center, radius = size.maxDimension * 0.75f,
            ),
        )
    }
}

/** Tarjeta de papel: fondo sólido, filete de tinta y título en versalitas con una línea debajo. */
@Composable
fun PaperCard(title: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val st = LocalStyle.current
    Column(modifier.fillMaxWidth().clip(PaperShape).background(st.card).border(1.dp, st.line, PaperShape).padding(16.dp)) {
        if (title != null) {
            SectionTitle(title)
            Spacer(Modifier.height(6.dp))
            Rule()
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text.uppercase(), fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = LocalStyle.current.accent)
}

@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalStyle.current.line.copy(alpha = 0.7f)))
}

/** Fila de ajustes: ícono, título (+ subtítulo), valor y chevron opcional. */
@Composable
fun PaperRow(glyph: Glyph, title: String, subtitle: String? = null, value: String? = null, chevron: Boolean = false, onClick: (() -> Unit)? = null) {
    val st = LocalStyle.current
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiGlyph(glyph, st.accent, 22.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = st.ink)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = st.inkSoft)
        }
        if (value != null) Text(value, fontSize = 15.sp, color = st.inkSoft)
        if (chevron) {
            Spacer(Modifier.width(6.dp))
            UiGlyph(Glyph.Chevron, st.inkSoft, 18.dp)
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    val st = LocalStyle.current
    Column(Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTap(Glyph.Back, onBack)
            Spacer(Modifier.width(4.dp))
            Text(title, fontSize = 22.sp, color = st.ink, fontWeight = FontWeight.Medium)
        }
        Rule(Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
fun IconTap(glyph: Glyph, onClick: () -> Unit, tint: Color = LocalStyle.current.ink) {
    Box(Modifier.size(48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) { UiGlyph(glyph, tint, 24.dp) }
}
