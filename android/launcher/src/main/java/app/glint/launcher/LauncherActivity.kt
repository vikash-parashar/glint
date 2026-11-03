package app.glint.launcher

import android.Manifest
import android.content.Intent
import android.os.Bundle
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
import app.glint.launcher.ui.LauncherRoot
import kotlinx.coroutines.launch

class LauncherActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private val dock by lazy { DockAutoStarter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val home by vm.home.collectAsStateWithLifecycle()
            val overlays by vm.overlays.collectAsStateWithLifecycle()
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
                onOpenSettings = {
                    startActivity(Intent(ACTION_SETTINGS).setPackage(packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                onGrantDnd = { startActivity(vm.focus.dnd.accessSettingsIntent()) },
                hasDndAccess = { vm.focus.dnd.hasAccess },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
        dock.onResume()   // charging + auto-rotate on + set on its side = bedside clock
    }

    override fun onPause() {
        super.onPause()
        dock.onPause()
    }

    /** Pressing Home while already home closes the drawer instead of doing nothing. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.openDrawer(false)
        vm.openFocusSetup(false)
    }

    private fun start(app: AppEntry) {
        runCatching { startActivity(app.launch) }
    }

    companion object { const val ACTION_SETTINGS = "app.glint.action.SETTINGS" }
}
