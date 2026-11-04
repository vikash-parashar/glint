package app.glint.launcher

import android.Manifest
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.glint.dock.DockAutoStarter
import app.glint.launcher.data.AppEntry
import app.glint.launcher.data.GlintWidgetHost
import app.glint.launcher.ui.LauncherRoot
import app.glint.widgets.catalog.Needs
import app.glint.widgets.catalog.WidgetCatalog
import app.glint.widgets.engine.GlintWidget
import app.glint.widgets.store.WidgetConfig
import app.glint.widgets.store.WidgetConfigStore
import kotlinx.coroutines.launch

/**
 * Glint is a launcher first. The app icon and the Home button both land here.
 * Widgets live on this home screen (added from the long-press menu); Dock is a Pro extra.
 */
class LauncherActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private val dock by lazy { DockAutoStarter(this) }
    private val widgetHost by lazy { GlintWidgetHost(this) }

    /** Widget waiting for the one-time "allow Glint to create widgets" answer. */
    private var pendingBind: Pair<Int, String>? = null

    private val bindConsent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val (id, design) = pendingBind ?: return@registerForActivityResult
        pendingBind = null
        if (res.resultCode == RESULT_OK) finishAdd(id, design) else widgetHost.host.deleteAppWidgetId(id)
    }

    private val homeRole = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.setDefaultHome(isDefaultHome())
        // Some phones (MIUI, ColorOS) ignore the role dialog: send people to the system screen instead.
        if (!isDefaultHome()) runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { b ->
            val id = b.getInt("bind_id", -1); val d = b.getString("bind_design")
            if (id >= 0 && d != null) pendingBind = id to d
        }
        setContent {
            val home by vm.home.collectAsStateWithLifecycle()
            val overlays by vm.overlays.collectAsStateWithLifecycle()
            val widgetIds by vm.widgetIds.collectAsStateWithLifecycle()
            val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                vm.onCalendarPermissionResult()
            }
            LauncherRoot(
                home = home,
                overlays = overlays,
                vm = vm,
                onLaunch = { app -> lifecycleScope.launch { vm.requestLaunch(app)?.let(::start) } },
                onForceLaunch = { app -> lifecycleScope.launch { start(vm.grantPassAndLaunch(app)) } },
                onAskCalendar = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
                onOpenSettings = { openSettings(null) },
                onGrantDnd = { startActivity(vm.focus.dnd.accessSettingsIntent()) },
                hasDndAccess = { vm.focus.dnd.hasAccess },
                widgetIds = widgetIds,
                host = widgetHost,
                onAddWidget = { openSettings("Widgets") },
                onWallpaper = { openSettings("Wallpapers") },
                onDock = { openSettings("Dock") },
                onSetHome = ::requestHome,
            )
        }
        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingBind?.let { outState.putInt("bind_id", it.first); outState.putString("bind_design", it.second) }
    }

    override fun onStart() {
        super.onStart()
        runCatching { widgetHost.start() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { widgetHost.stop() }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
        vm.setDefaultHome(isDefaultHome())
        dock.onResume()   // Pro: charging + auto-rotate on + set on its side = bedside clock
    }

    override fun onPause() {
        super.onPause()
        dock.onPause()
    }

    /** Pressing Home while already home closes the drawer instead of doing nothing. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!handleIntent(intent)) {
            vm.openDrawer(false)
            vm.openFocusSetup(false)
            vm.openMenu(false)
        }
    }

    /** The widget gallery sends ACTION_ADD_WIDGET with a design id. */
    private fun handleIntent(intent: Intent?): Boolean {
        if (intent?.action != ACTION_ADD_WIDGET) return false
        val designId = intent.getStringExtra(EXTRA_DESIGN) ?: return false
        intent.action = null
        addWidget(designId)
        return true
    }

    private fun addWidget(designId: String) {
        val design = WidgetCatalog.byId(designId) ?: return
        val id = widgetHost.host.allocateAppWidgetId()
        val provider = ComponentName(packageName, "app.glint.widgets.engine.${design.kind.receiver}")
        if (widgetHost.manager.bindAppWidgetIdIfAllowed(id, provider)) {
            finishAdd(id, designId)
        } else {
            pendingBind = id to designId
            bindConsent.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider),
            )
        }
    }

    private fun finishAdd(id: Int, designId: String) = lifecycleScope.launch {
        WidgetConfigStore(this@LauncherActivity).put(id, WidgetConfig(designId))
        vm.addWidget(id)
        GlintWidget.refresh(this@LauncherActivity, id)
        // Widgets that need a city, a permission, a photo… open their setup right away.
        val needs = WidgetCatalog.byId(designId)?.needs ?: Needs.Nothing
        if (needs != Needs.Nothing) {
            startActivity(
                Intent().setClassName(packageName, "app.glint.widgets.config.WidgetConfigActivity")
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            )
        }
    }

    private fun openSettings(screen: String?) {
        runCatching {
            startActivity(
                Intent(ACTION_SETTINGS).setPackage(packageName)
                    .apply { if (screen != null) putExtra("screen", screen) }
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
    }

    private fun isDefaultHome(): Boolean {
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_HOME)) return rm.isRoleHeld(RoleManager.ROLE_HOME)
        val resolved = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        return resolved?.activityInfo?.packageName == packageName
    }

    private fun requestHome() {
        val rm = getSystemService(RoleManager::class.java)
        val intent = if (rm.isRoleAvailable(RoleManager.ROLE_HOME)) rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
        else Intent(Settings.ACTION_HOME_SETTINGS)
        runCatching { homeRole.launch(intent) }.onFailure { runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) } }
    }

    private fun start(app: AppEntry) {
        runCatching { startActivity(app.launch) }
    }

    companion object {
        const val ACTION_SETTINGS = "app.glint.action.SETTINGS"
        const val ACTION_ADD_WIDGET = "app.glint.action.ADD_WIDGET"
        const val EXTRA_DESIGN = "design"
    }
}
