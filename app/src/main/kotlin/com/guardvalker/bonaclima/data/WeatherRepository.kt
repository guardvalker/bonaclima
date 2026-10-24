package com.guardvalker.bonaclima.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.guardvalker.bonaclima.domain.Forecast
import com.guardvalker.bonaclima.domain.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

private val Context.dataStore by preferencesDataStore("bonaclima")

val DEFAULT_PLACE = Place("Buenos Aires", "Argentina", -34.6037, -58.3816)

/** Único punto de acceso de la UI y del widget a los datos: provider activo + lugar guardado + caché en disco. */
class WeatherRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, "forecast.json")

    // Provider activo; cambiar de fuente = instanciar otro acá (o elegirlo desde Ajustes más adelante).
    private val open = OpenMeteoProvider()
    private val weather: WeatherProvider = open
    private val geocoding: GeocodingProvider = open

    private object Keys {
        val name = stringPreferencesKey("place_name")
        val region = stringPreferencesKey("place_region")
        val lat = doublePreferencesKey("place_lat")
        val lon = doublePreferencesKey("place_lon")
    }

    suspend fun savedPlace(): Place {
        val p = appContext.dataStore.data.first()
        val name = p[Keys.name] ?: return DEFAULT_PLACE
        return Place(name, p[Keys.region], p[Keys.lat] ?: return DEFAULT_PLACE, p[Keys.lon] ?: return DEFAULT_PLACE)
    }

    suspend fun savePlace(place: Place) {
        appContext.dataStore.edit {
            it[Keys.name] = place.name
            if (place.region != null) it[Keys.region] = place.region else it.remove(Keys.region)
            it[Keys.lat] = place.latitude
            it[Keys.lon] = place.longitude
        }
    }

    suspend fun cached(): Forecast? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString<Forecast>(cacheFile.readText()) }.getOrNull()
    }

    suspend fun fetch(place: Place): Forecast {
        val forecast = weather.forecast(place)
        withContext(Dispatchers.IO) { runCatching { cacheFile.writeText(json.encodeToString(forecast)) } }
        return forecast
    }

    suspend fun search(query: String): List<Place> = geocoding.search(query)
}
