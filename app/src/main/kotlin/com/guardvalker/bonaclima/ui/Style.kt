package com.guardvalker.bonaclima.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Estilo visual "papel". Cada estilo define la hoja, la tinta y los colores del cielo de la viñeta animada.
 * Para sumar uno nuevo alcanza con agregarlo a [STYLES].
 */
data class BonaStyle(
    val id: String,
    val name: String,
    val tagline: String,
    val dark: Boolean,
    val paper: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val accent: Color,
    val line: Color,
    /** Azul tinta, para lluvia/agua. */
    val water: Color,
    /** Hoja rayada de cuaderno. */
    val ruled: Boolean,
    val dayTop: Color,
    val dayBottom: Color,
    val nightTop: Color,
    val nightBottom: Color,
    val twiTop: Color,
    val twiBottom: Color,
    val overcast: Color,
    val cloud: Color,
)

val STYLES = listOf(
    BonaStyle(
        id = "pergamino", name = "Pergamino", tagline = "Hoja crema, tinta marrón", dark = false,
        paper = Color(0xFFF0E2C2), card = Color(0xFFF7EDD6), ink = Color(0xFF3B2716), inkSoft = Color(0xFF7A6350),
        accent = Color(0xFF8A4B2A), line = Color(0xFFB9A27C), water = Color(0xFF3F6A87), ruled = false,
        dayTop = Color(0xFFE3B66E), dayBottom = Color(0xFFF4DDA6), nightTop = Color(0xFF2B1D14), nightBottom = Color(0xFF4E3624),
        twiTop = Color(0xFF6B4A3A), twiBottom = Color(0xFFE39A5C), overcast = Color(0xFFB7A58C), cloud = Color(0xFFFFF4DC),
    ),
    BonaStyle(
        id = "cuaderno", name = "Cuaderno", tagline = "Hoja rayada con margen", dark = false,
        paper = Color(0xFFFBF5E6), card = Color(0xFFFFFBEF), ink = Color(0xFF4A3426), inkSoft = Color(0xFF8A7566),
        accent = Color(0xFFB5532F), line = Color(0xFFD8B9A0), water = Color(0xFF4A7390), ruled = true,
        dayTop = Color(0xFFEDB89A), dayBottom = Color(0xFFFBE3CF), nightTop = Color(0xFF3A2A33), nightBottom = Color(0xFF6A4A4E),
        twiTop = Color(0xFF7A4E5A), twiBottom = Color(0xFFF0A27A), overcast = Color(0xFFC9B8AE), cloud = Color(0xFFFFF8F0),
    ),
    BonaStyle(
        id = "cuero", name = "Cuero", tagline = "Tapa de cuero y letras doradas", dark = true,
        paper = Color(0xFF3A2515), card = Color(0xFF4A3120), ink = Color(0xFFF1E1C1), inkSoft = Color(0xFFBFA580),
        accent = Color(0xFFD9A441), line = Color(0xFF7A5A3C), water = Color(0xFF7FB0CF), ruled = false,
        dayTop = Color(0xFFB8793E), dayBottom = Color(0xFFE6B877), nightTop = Color(0xFF1E130B), nightBottom = Color(0xFF3A2616),
        twiTop = Color(0xFF5A3326), twiBottom = Color(0xFFD98A4E), overcast = Color(0xFF8A765E), cloud = Color(0xFFE9D9B8),
    ),
    BonaStyle(
        id = "sepia", name = "Sepia", tagline = "Foto vieja, tonos ámbar", dark = false,
        paper = Color(0xFFD8BE98), card = Color(0xFFE5D0AC), ink = Color(0xFF2E1E12), inkSoft = Color(0xFF6B5238),
        accent = Color(0xFF7A3E1D), line = Color(0xFF9C805A), water = Color(0xFF3E6684), ruled = false,
        dayTop = Color(0xFFC98F4F), dayBottom = Color(0xFFE8C690), nightTop = Color(0xFF2A1B10), nightBottom = Color(0xFF4A3322),
        twiTop = Color(0xFF5E3A2A), twiBottom = Color(0xFFD9894A), overcast = Color(0xFFA8957A), cloud = Color(0xFFF3E3C0),
    ),
    BonaStyle(
        id = "tinta", name = "Tinta", tagline = "Biblioteca de noche", dark = true,
        paper = Color(0xFF1E1612), card = Color(0xFF2B201A), ink = Color(0xFFEADCC0), inkSoft = Color(0xFFA8957A),
        accent = Color(0xFFC98B4A), line = Color(0xFF5A4636), water = Color(0xFF86B3CC), ruled = false,
        dayTop = Color(0xFF8C5A34), dayBottom = Color(0xFFC79A62), nightTop = Color(0xFF0F0A07), nightBottom = Color(0xFF2B1D15),
        twiTop = Color(0xFF4A2C26), twiBottom = Color(0xFFC27440), overcast = Color(0xFF6E5E4E), cloud = Color(0xFFD8C7A5),
    ),
)

fun styleById(id: String?) = STYLES.firstOrNull { it.id == id } ?: STYLES.first()

val LocalStyle = staticCompositionLocalOf { STYLES.first() }
