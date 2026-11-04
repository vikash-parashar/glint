package app.glint.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.glint.focus.FocusController
import app.glint.focus.FocusSession
import app.glint.focus.FocusStats
import app.glint.focus.FocusStatsCalculator
import app.glint.data.AgendaRepository
import app.glint.launcher.data.AppEntry
import app.glint.launcher.data.AppRepository
import app.glint.data.BatteryState
import app.glint.data.CityStore
import app.glint.launcher.data.LauncherPrefs
import app.glint.data.NextEvent
import app.glint.data.Weather
import app.glint.data.WeatherRepository
import app.glint.data.batteryFlow
import app.glint.theme.GlintPalette
import app.glint.theme.GlintStyles
import app.glint.theme.StyleKind
import app.glint.theme.StyleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeData(
    val style: GlintPalette = GlintStyles.Bento,
    val apps: List<AppEntry> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val battery: BatteryState? = null,
    val session: FocusSession? = null,
    val stats: FocusStats = FocusStats(0, 0, List(7) { 0 }),
    val defaultAllowed: Set<String> = emptySet(),
)

/** One-off things the UI shows on top of home. */
data class Overlays(
    val drawerOpen: Boolean = false,
    val focusSetupOpen: Boolean = false,
    val blocked: AppEntry? = null,
    val summary: FocusSession? = null,
    val weather: Weather? = null,
    val city: String? = null,
    val nextEvent: NextEvent? = null,
    val calendarGranted: Boolean = false,
    val menuOpen: Boolean = false,
    val editingWidgets: Boolean = false,
    val isDefaultHome: Boolean = true,
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val appsRepo = AppRepository(app)
    private val prefs = LauncherPrefs(app)
    private val styles = StyleRepository(app)
    private val cityStore = CityStore(app)
    private val agenda = AgendaRepository(app)
    val focus = FocusController(app)

    private val homeCore = combine(
        styles.active, appsRepo.apps(), prefs.favorites, batteryFlow(app), focus.repo.active,
    ) { style, apps, favs, battery, session ->
        val byPkg = apps.associateBy { it.packageName }
        val favPkgs = favs ?: appsRepo.defaultFavorites()
        HomeData(
            style = style, apps = apps, battery = battery, session = session,
            favorites = favPkgs.mapNotNull { byPkg[it] },
        )
    }

    val home: StateFlow<HomeData> = combine(homeCore, focus.repo.history, focus.repo.defaultAllowed) { h, hist, allowed ->
        h.copy(stats = FocusStatsCalculator.compute(hist, System.currentTimeMillis()), defaultAllowed = allowed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeData())

    /** Widgets the user placed on the Glint home. */
    val widgetIds: StateFlow<List<Int>> = prefs.widgets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    suspend fun addWidget(id: Int) = prefs.addWidget(id)
    fun removeWidget(id: Int) = viewModelScope.launch { prefs.removeWidget(id) }
    fun moveWidget(id: Int, up: Boolean) = viewModelScope.launch { prefs.moveWidget(id, up) }

    private val _overlays = MutableStateFlow(Overlays())
    val overlays: StateFlow<Overlays> = _overlays.asStateFlow()

    init {
        viewModelScope.launch { cityStore.city.collect { c -> _overlays.update { it.copy(city = c) }; refreshWeather() } }
    }

    fun onResume() = viewModelScope.launch {
        focus.endIfOverdue()?.let { done -> _overlays.update { it.copy(summary = done) } }
        _overlays.update { it.copy(drawerOpen = false, calendarGranted = agenda.hasPermission()) }
        _overlays.update { it.copy(nextEvent = agenda.next()) }
        refreshWeather()
    }

    private suspend fun refreshWeather() {
        val city = _overlays.value.city ?: return
        WeatherRepository.forCity(city)?.let { w -> _overlays.update { it.copy(weather = w) } }
    }

    /** Returns the app to start, or null if focus mode blocked it (the block sheet opens). */
    suspend fun requestLaunch(app: AppEntry): AppEntry? =
        when (focus.canLaunch(app.packageName)) {
            FocusController.LaunchDecision.Allowed -> app
            FocusController.LaunchDecision.Blocked -> {
                focus.repo.recordBlockedOpen()
                _overlays.update { it.copy(blocked = app) }
                null
            }
        }

    suspend fun grantPassAndLaunch(app: AppEntry): AppEntry {
        focus.repo.grantPass(app.packageName, System.currentTimeMillis())
        _overlays.update { it.copy(blocked = null) }
        return app
    }

    fun startFocus(intent: String, minutes: Int, allowed: Set<String>, silence: Boolean) = viewModelScope.launch {
        focus.start(intent, minutes, allowed, silence)
        _overlays.update { it.copy(focusSetupOpen = false, drawerOpen = false) }
    }

    fun stopFocus() = viewModelScope.launch {
        val done = focus.stop()
        _overlays.update { it.copy(summary = done) }
    }

    fun setStyle(kind: StyleKind) = viewModelScope.launch {
        styles.set(kind)
        app.glint.widgets.engine.GlintWidget.refreshAll(getApplication())
    }
    fun setCity(city: String) = viewModelScope.launch { cityStore.set(city) }

    fun toggleFavorite(pkg: String) = viewModelScope.launch {
        val cur = home.value.favorites.map { it.packageName }
        prefs.setFavorites(if (pkg in cur) cur - pkg else cur + pkg)
    }

    fun onCalendarPermissionResult() = viewModelScope.launch {
        _overlays.update { it.copy(calendarGranted = agenda.hasPermission(), nextEvent = agenda.next()) }
    }

    fun openMenu(open: Boolean) = _overlays.update { it.copy(menuOpen = open) }
    fun editWidgets(on: Boolean) = _overlays.update { it.copy(editingWidgets = on, menuOpen = false) }
    fun setDefaultHome(isHome: Boolean) = _overlays.update { it.copy(isDefaultHome = isHome) }

    fun openDrawer(open: Boolean) = _overlays.update { it.copy(drawerOpen = open) }
    fun openFocusSetup(open: Boolean) = _overlays.update { it.copy(focusSetupOpen = open) }
    fun dismissBlocked() = _overlays.update { it.copy(blocked = null) }
    fun dismissSummary() = _overlays.update { it.copy(summary = null) }
}
