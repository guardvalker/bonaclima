package com.guardvalker.bonaclima.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.LocalDateTime

/** Condición normalizada: cada provider traduce sus códigos a esto; UI, fondos y widget solo conocen esto. */
enum class Condition(val label: String, val dayEmoji: String, val nightEmoji: String = dayEmoji) {
    Clear("Despejado", "☀️", "🌙"),
    PartlyCloudy("Parcialmente nublado", "⛅", "☁️"),
    Cloudy("Nublado", "☁️"),
    Fog("Niebla", "🌫️"),
    Drizzle("Llovizna", "🌦️", "🌧️"),
    Rain("Lluvia", "🌧️"),
    Snow("Nieve", "❄️"),
    Thunderstorm("Tormenta", "⛈️"),
    ;

    fun emoji(isDay: Boolean) = if (isDay) dayEmoji else nightEmoji
}

@Serializable
data class Place(
    val name: String,
    val region: String? = null,
    val latitude: Double,
    val longitude: Double,
) {
    fun sameSpot(other: Place) =
        kotlin.math.abs(latitude - other.latitude) < 0.01 && kotlin.math.abs(longitude - other.longitude) < 0.01
}

@Serializable
data class CurrentWeather(
    @Serializable(LocalDateTimeSerializer::class) val time: LocalDateTime,
    val temperature: Double,
    val apparentTemperature: Double,
    val condition: Condition,
    val isDay: Boolean,
    val humidity: Int? = null,
    val windSpeedKmh: Double? = null,
    val windDirectionDeg: Int? = null,
    val pressureHpa: Double? = null,
    val uvIndex: Double? = null,
    val precipitationMm: Double? = null,
)

@Serializable
data class HourlyPoint(
    @Serializable(LocalDateTimeSerializer::class) val time: LocalDateTime,
    val temperature: Double,
    val condition: Condition,
    val isDay: Boolean,
    val precipProbability: Int? = null,
)

@Serializable
data class DailyPoint(
    @Serializable(LocalDateSerializer::class) val date: LocalDate,
    val condition: Condition,
    val tempMin: Double,
    val tempMax: Double,
    @Serializable(LocalDateTimeSerializer::class) val sunrise: LocalDateTime? = null,
    @Serializable(LocalDateTimeSerializer::class) val sunset: LocalDateTime? = null,
    val precipProbability: Int? = null,
    val uvMax: Double? = null,
)

@Serializable
data class Forecast(
    val place: Place,
    val providerId: String,
    val fetchedAtMillis: Long,
    val current: CurrentWeather,
    val hourly: List<HourlyPoint>,
    val daily: List<DailyPoint>,
)

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
}

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}
