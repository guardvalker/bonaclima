package com.guardvalker.bonaclima.ui

class ChangelogEntry(val version: String, val date: String, val items: List<String>)

/** Historial de cambios que muestra la app. Agregar una entrada arriba en cada release. */
val CHANGELOG = listOf(
    ChangelogEntry(
        "0.3.2", "2026-10-25",
        listOf(
            "Franjas: orden fijo Mañana, Tarde, Tardecita, Noche; las franjas ya no cambian de lugar.",
            "Deslizando sobre las franjas se abre el módulo del momento y se cierra el anterior, subiendo o bajando de a poco.",
            "Tocar una franja la abre con animación. Deslizar fuera de las franjas scrollea toda la pantalla.",
            "La ciudad y los ajustes quedan fijos arriba, sobre el papel.",
        ),
    ),
    ChangelogEntry(
        "0.3.1", "2026-10-25",
        listOf(
            "Franjas rediseñado: arriba una sola franja grande que cambia de color y de escena al deslizar el número; las demás quedan plegadas (nombre y temperatura chica).",
            "Se ven también las próximas horas y los próximos días en la misma pantalla.",
            "Arreglado el número grande que se cortaba y los textos que se pisaban.",
        ),
    ),
    ChangelogEntry(
        "0.3.0", "2026-10-25",
        listOf(
            "Nuevo estilo \"Franjas\": número grande y franjas de color para mañana, tarde, tardecita y noche.",
            "Deslizá hacia abajo desde el número para recorrer las horas del día: cambian la temperatura, el clima y el color del cielo.",
            "Cada clima tiene su escena animada: sol radiante, luna con estrellas, nubes que flotan, gotas, copos, tormenta con rayo y niebla.",
            "Tocá una franja para saltar a ese momento del día.",
        ),
    ),
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
