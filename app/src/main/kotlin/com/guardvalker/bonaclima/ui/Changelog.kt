package com.guardvalker.bonaclima.ui

class ChangelogEntry(val version: String, val date: String, val items: List<String>)

/** Historial de cambios que muestra la app. Agregar una entrada arriba en cada release. */
val CHANGELOG = listOf(
    ChangelogEntry(
        "0.2.0", "2026-10-24",
        listOf(
            "Nuevo estilo de papel: hoja crema, tinta marrón, tipografía con serif y tarjetas sólidas.",
            "5 estilos para elegir: Pergamino, Cuaderno, Cuero, Sepia y Tinta.",
            "Íconos dibujados a trazo de tinta, sin emojis.",
            "El cielo animado ahora vive en una viñeta enmarcada.",
            "Ajustes (arriba a la derecha): estilo, versión, buscar actualizaciones e historial de cambios.",
        ),
    ),
    ChangelogEntry(
        "0.1.0", "2026-10-24",
        listOf(
            "Primera versión: pronóstico con Open-Meteo, horas y 8 días, detalles.",
            "Fondo animado según el clima y la hora (amanecer, atardecer, noche).",
            "Búsqueda de ciudad y ubicación actual, con caché sin conexión.",
            "Auto-actualización desde GitHub.",
        ),
    ),
)
