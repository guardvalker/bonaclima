package com.guardvalker.bonaclima.ui

class ChangelogEntry(val version: String, val date: String, val items: List<String>)

/** Historial de cambios que muestra la app. Agregar una entrada arriba en cada release. */
val CHANGELOG = listOf(
    ChangelogEntry(
        "0.4.0", "2026-10-26",
        listOf(
            "Nuevo estilo \"Google\" (ahora el predeterminado): diseño tipo Google Weather con colores Material You del sistema, claro/oscuro automático y tarjetas redondeadas.",
            "Resumen en una frase, gráfico de temperatura por hora, pronóstico de 10 días y tarjetas de precipitación, viento con ráfagas, UV, humedad con punto de rocío, calidad del aire, visibilidad, presión y arco del sol.",
            "Los estilos de papel y Franjas siguen disponibles en Ajustes.",
        ),
    ),
    ChangelogEntry(
        "0.3.10", "2026-10-26",
        listOf(
            "Franjas: las próximas horas se tiñen con el color de su momento del día (mañana, tarde, tardecita, noche) y muestran el nombre donde empieza cada uno.",
            "Curva suave del sol de fondo: marca la posición del sol (o la luna) en cada hora, con el horizonte y la hora actual resaltada como \"Ahora\".",
        ),
    ),
    ChangelogEntry(
        "0.3.9", "2026-10-26",
        listOf(
            "Franjas: cada muestra del selector ahora dice su nombre, la temperatura y el rango de horas (ej. Tarde 12° 12–18 h).",
        ),
    ),
    ChangelogEntry(
        "0.3.8", "2026-10-26",
        listOf(
            "Franjas: el cambio de momento ahora es un barrido; el color nuevo empuja una línea sobre el anterior, y el texto cambia de contraste justo donde pasa la línea.",
        ),
    ),
    ChangelogEntry(
        "0.3.7", "2026-10-26",
        listOf(
            "Franjas: el cambio de momento ahora es un snap limpio, una franja por gesto, sin recorrer horas intermedias.",
            "El panel acompaña un poco al dedo, con más resistencia, y salta solo a la franja contigua al pasar el umbral; si soltás antes, vuelve.",
            "Animación única de 300 ms entre franjas, sin movimientos superpuestos.",
        ),
    ),
    ChangelogEntry(
        "0.3.6", "2026-10-25",
        listOf(
            "Franjas: un solo panel con el número mucho más grande; el número y la escena ya no se mueven de lugar.",
            "Las franjas pasaron a ser un selector de color al costado: al elegir otra cambian el color, los valores y el nombre (Mañana, Tarde, Tardecita, Noche).",
            "Se puede elegir tocando el selector o deslizando sobre el panel, con el mismo snap de antes.",
        ),
    ),
    ChangelogEntry(
        "0.3.5", "2026-10-25",
        listOf(
            "Franjas: las franjas plegadas son más finas y la activa es más alta, con la temperatura mucho más grande.",
            "Se quitó el mensaje que explicaba cómo deslizar.",
        ),
    ),
    ChangelogEntry(
        "0.3.4", "2026-10-25",
        listOf(
            "Franjas: al arrastrar despacio sobre las franjas ya no se mueve toda la pantalla (el gesto se toma antes que el scroll).",
        ),
    ),
    ChangelogEntry(
        "0.3.3", "2026-10-25",
        listOf(
            "Franjas con snap: un gesto sobre las franjas mueve el módulo abierto a la franja contigua (se acompaña un poco con el dedo y luego salta solo).",
            "Dirección invertida: dedo hacia abajo = franja anterior, hacia arriba = la siguiente.",
            "Arreglados los textos superpuestos al cambiar de franja.",
            "La ciudad y los ajustes quedan fijos y ya no se meten bajo la barra de estado al scrollear.",
        ),
    ),
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
