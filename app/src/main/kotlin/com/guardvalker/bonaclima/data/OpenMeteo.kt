package com.guardvalker.bonaclima.data

import com.guardvalker.bonaclima.domain.Condition
import com.guardvalker.bonaclima.domain.CurrentWeather
import com.guardvalker.bonaclima.domain.DailyPoint
import com.guardvalker.bonaclima.domain.Forecast
import com.guardvalker.bonaclima.domain.HourlyPoint
import com.guardvalker.bonaclima.domain.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

internal val http = OkHttpClient()
internal val json = Json { ignoreUnknownKeys = true }

internal suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(url.toHttpUrl()).header("User-Agent", "BonaClima/0.1").build()
    http.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        response.body.string()
    }
}

class OpenMeteoProvider : WeatherProvider, GeocodingProvider {
    override val id = "open-meteo"
    override val displayName = "Open-Meteo"

    override suspend fun forecast(place: Place): Forecast {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation," +
            "weather_code,wind_speed_10m,wind_direction_10m,pressure_msl,uv_index" +
            "&hourly=temperature_2m,precipitation_probability,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max," +
            "sunrise,sunset,uv_index_max" +
            "&timezone=auto&forecast_days=8"
        val dto = json.decodeFromString<ForecastDto>(httpGet(url))
        return dto.toForecast(place, id)
    }

    override suspend fun search(query: String): List<Place> {
        if (query.isBlank()) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query.trim())
            .addQueryParameter("count", "8")
            .addQueryParameter("language", "es")
            .build().toString()
        return json.decodeFromString<GeocodingDto>(httpGet(url)).results.orEmpty().map {
            Place(
                name = it.name,
                region = listOfNotNull(it.admin1, it.country).joinToString(", ").ifBlank { null },
                latitude = it.latitude,
                longitude = it.longitude,
            )
        }
    }
}

/** Códigos WMO → condición normalizada. */
internal fun wmoToCondition(code: Int?): Condition = when (code) {
    0, 1 -> Condition.Clear
    2 -> Condition.PartlyCloudy
    3 -> Condition.Cloudy
    45, 48 -> Condition.Fog
    51, 53, 55, 56, 57 -> Condition.Drizzle
    61, 63, 65, 66, 67, 80, 81, 82 -> Condition.Rain
    71, 73, 75, 77, 85, 86 -> Condition.Snow
    95, 96, 99 -> Condition.Thunderstorm
    else -> Condition.Cloudy
}

@Serializable
private data class GeocodingDto(val results: List<GeoResultDto>? = null)

@Serializable
private data class GeoResultDto(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
)

@Serializable
private data class ForecastDto(
    val current: CurrentDto,
    val hourly: HourlyDto,
    val daily: DailyDto,
) {
    fun toForecast(place: Place, providerId: String): Forecast {
        val cur = CurrentWeather(
            time = LocalDateTime.parse(current.time),
            temperature = current.temperature ?: 0.0,
            apparentTemperature = current.apparent ?: current.temperature ?: 0.0,
            condition = wmoToCondition(current.code),
            isDay = current.isDay != 0,
            humidity = current.humidity,
            windSpeedKmh = current.windSpeed,
            windDirectionDeg = current.windDirection,
            pressureHpa = current.pressure,
            uvIndex = current.uv,
            precipitationMm = current.precipitation,
        )
        val hours = hourly.time.indices.mapNotNull { i ->
            val t = hourly.temperature.getOrNull(i) ?: return@mapNotNull null
            HourlyPoint(
                time = LocalDateTime.parse(hourly.time[i]),
                temperature = t,
                condition = wmoToCondition(hourly.code.getOrNull(i)),
                isDay = hourly.isDay.getOrNull(i) != 0,
                precipProbability = hourly.precipProbability.getOrNull(i),
            )
        }
        val days = daily.time.indices.mapNotNull { i ->
            val max = daily.max.getOrNull(i) ?: return@mapNotNull null
            val min = daily.min.getOrNull(i) ?: return@mapNotNull null
            DailyPoint(
                date = LocalDate.parse(daily.time[i]),
                condition = wmoToCondition(daily.code.getOrNull(i)),
                tempMin = min,
                tempMax = max,
                sunrise = daily.sunrise.getOrNull(i)?.let(LocalDateTime::parse),
                sunset = daily.sunset.getOrNull(i)?.let(LocalDateTime::parse),
                precipProbability = daily.precipProbability.getOrNull(i),
                uvMax = daily.uvMax.getOrNull(i),
            )
        }
        return Forecast(place, providerId, System.currentTimeMillis(), cur, hours, days)
    }
}

@Serializable
private data class CurrentDto(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("apparent_temperature") val apparent: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Int? = null,
    @SerialName("is_day") val isDay: Int? = 1,
    val precipitation: Double? = null,
    @SerialName("weather_code") val code: Int? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Int? = null,
    @SerialName("pressure_msl") val pressure: Double? = null,
    @SerialName("uv_index") val uv: Double? = null,
)

@Serializable
private data class HourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipProbability: List<Int?> = emptyList(),
    @SerialName("weather_code") val code: List<Int?> = emptyList(),
    @SerialName("is_day") val isDay: List<Int?> = emptyList(),
)

@Serializable
private data class DailyDto(
    val time: List<String>,
    @SerialName("weather_code") val code: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val max: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val min: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipProbability: List<Int?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
    @SerialName("uv_index_max") val uvMax: List<Double?> = emptyList(),
)
