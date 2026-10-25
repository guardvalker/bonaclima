package com.guardvalker.bonaclima.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardvalker.bonaclima.data.AppUpdater
import com.guardvalker.bonaclima.data.DEFAULT_PLACE
import com.guardvalker.bonaclima.data.WeatherRepository
import com.guardvalker.bonaclima.domain.Forecast
import com.guardvalker.bonaclima.domain.Place
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val place: Place = DEFAULT_PLACE,
    val forecast: Forecast? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
    val searchResults: List<Place> = emptyList(),
    val styleId: String = STYLES.first().id,
    /** Mensaje de la última búsqueda de actualización de la app (null = nada que mostrar). */
    val updateStatus: String? = null,
    val checkingUpdate: Boolean = false,
)

class WeatherViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WeatherRepository(app)
    private val updater = AppUpdater(app)
    val version: String = updater.currentVersion
    private val uiPrefs = app.getSharedPreferences("ui", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(UiState(styleId = styleById(uiPrefs.getString("style", null)).id))
    val state: StateFlow<UiState> = _state

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            val place = repo.savedPlace()
            val cached = repo.cached()?.takeIf { it.place.sameSpot(place) }
            _state.update { it.copy(place = place, forecast = cached) }
            refresh()
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            val place = _state.value.place
            try {
                val forecast = repo.fetch(place)
                _state.update { it.copy(forecast = forecast, refreshing = false) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(refreshing = false, error = "No se pudo actualizar (${e.message ?: "sin conexión"})") }
            }
        }
    }

    fun setStyle(id: String) {
        uiPrefs.edit().putString("style", id).apply()
        _state.update { it.copy(styleId = id) }
    }

    /**
     * Revisa si hay un release nuevo en GitHub y lo instala. Automático: como máximo una vez por hora y en silencio.
     * Manual (desde Ajustes): siempre consulta y deja el resultado en [UiState.updateStatus].
     */
    fun checkForAppUpdate(manual: Boolean = false) {
        if (manual && _state.value.checkingUpdate) return
        viewModelScope.launch {
            if (manual) _state.update { it.copy(checkingUpdate = true, updateStatus = "Buscando…") }
            val status = try {
                when (val r = updater.checkAndInstall(force = manual)) {
                    is AppUpdater.Result.Installing -> "Actualizando a v${r.version}…"
                    AppUpdater.Result.UpToDate -> "Ya tenés la última versión (v$version)"
                    AppUpdater.Result.Skipped -> if (manual) "Ya hay una búsqueda en curso" else null
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (manual) "No se pudo comprobar (${e.message ?: "sin conexión"})" else null
            }
            _state.update { it.copy(checkingUpdate = false, updateStatus = status ?: it.updateStatus) }
        }
    }

    fun refreshIfStale() {
        val f = _state.value.forecast ?: return
        if (System.currentTimeMillis() - f.fetchedAtMillis > 15 * 60_000) refresh()
    }

    fun search(query: String) {
        viewModelScope.launch {
            val results = runCatching { repo.search(query) }.getOrDefault(emptyList())
            _state.update { it.copy(searchResults = results) }
        }
    }

    fun select(place: Place) {
        viewModelScope.launch {
            repo.savePlace(place)
            _state.update { it.copy(place = place, forecast = it.forecast?.takeIf { f -> f.place.sameSpot(place) }, searchResults = emptyList()) }
            refresh()
        }
    }

    @SuppressLint("MissingPermission")
    fun useMyLocation() {
        val lm = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstNotNullOfOrNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        if (loc == null) {
            _state.update { it.copy(error = "No se pudo obtener la ubicación") }
            return
        }
        select(Place("Mi ubicación", null, loc.latitude, loc.longitude))
    }
}
