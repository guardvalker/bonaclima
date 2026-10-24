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
)

class WeatherViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WeatherRepository(app)
    private val updater = AppUpdater(app)
    val version: String = updater.currentVersion
    private val _state = MutableStateFlow(UiState())
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

    /** Revisa si hay un release nuevo en GitHub (como máximo una vez por hora) y lo instala. */
    fun checkForAppUpdate() {
        viewModelScope.launch { runCatching { updater.checkAndInstall() } }
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
