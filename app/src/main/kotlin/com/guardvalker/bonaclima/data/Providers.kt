package com.guardvalker.bonaclima.data

import com.guardvalker.bonaclima.domain.Forecast
import com.guardvalker.bonaclima.domain.Place

/** Fuente de pronóstico. Para sumar otra (MET Norway, etc.) alcanza con implementar esto. */
interface WeatherProvider {
    val id: String
    val displayName: String
    suspend fun forecast(place: Place): Forecast
}

interface GeocodingProvider {
    suspend fun search(query: String): List<Place>
}
