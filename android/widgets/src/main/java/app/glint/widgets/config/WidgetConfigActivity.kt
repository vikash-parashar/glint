package app.glint.widgets.config

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import app.glint.data.AgendaRepository
import app.glint.data.CityStore
import app.glint.data.NowPlaying
import app.glint.data.StepsRepository
import app.glint.widgets.catalog.Needs
import app.glint.widgets.catalog.WidgetCatalog
import app.glint.widgets.catalog.WidgetDesign
import app.glint.widgets.catalog.WidgetKind
import app.glint.widgets.engine.GlintWidget
import app.glint.widgets.store.ParamKeys
import app.glint.widgets.store.WidgetConfig
import app.glint.widgets.store.WidgetConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Setup screen for one placed widget. Opens when a widget is added from the system
 * picker (optional, the widget works with defaults) or when the user taps a widget
 * that still needs something (a city, a permission, a photo).
 */
class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val kind = GlintWidget.kindOf(this, appWidgetId) ?: run { finish(); return }

        lifecycleScope.launch {
            val existing = WidgetConfigStore(this@WidgetConfigActivity).get(appWidgetId)
                ?.takeIf { WidgetCatalog.byId(it.designId)?.kind == kind }
                ?: WidgetConfig(WidgetCatalog.default(kind).id)
            val city = CityStore(this@WidgetConfigActivity).get().orEmpty()
            setContent { GlintSetupTheme { ConfigScreen(kind, existing, city) } }
        }
    }

    private fun save(config: WidgetConfig, city: String?) {
        lifecycleScope.launch {
            if (!city.isNullOrBlank()) CityStore(this@WidgetConfigActivity).set(city)
            WidgetConfigStore(this@WidgetConfigActivity).put(appWidgetId, config)
            GlintWidget.refresh(this@WidgetConfigActivity, appWidgetId)
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }

    /** Copies the picked photo into app storage so the widget never needs storage permission. */
    private suspend fun importPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val out = File(filesDir, "widget_photos").apply { mkdirs() }.let { File(it, "w_$appWidgetId.jpg") }
            contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
            out.absolutePath
        }.getOrNull()
    }

    @Composable
    private fun ConfigScreen(kind: WidgetKind, initial: WidgetConfig, initialCity: String) {
        val ctx = this
        var designId by remember { mutableStateOf(initial.designId) }
        var params by remember { mutableStateOf(initial.params) }
        var city by remember { mutableStateOf(initialCity) }
        val design = WidgetCatalog.byId(designId) ?: WidgetCatalog.default(kind)
        var refreshTick by remember { mutableIntStateOf(0) }
        LifecycleResumeEffect(Unit) { refreshTick++; onPauseOrDispose { } }

        fun set(key: String, value: String) { params = params + (key to value) }

        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text("Set up ${kind.title.lowercase()}", color = MaterialTheme.colorScheme.onSurface, fontSize = 26.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
                Text("Pick a design. You can change it later by tapping and holding the widget.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)

                val options = WidgetCatalog.forKind(kind)
                if (options.size > 1) {
                    SectionTitle("Design")
                    options.forEach { o ->
                        ChoiceCard(o.name, o.blurb, o.id == designId, badge = if (o.pro) "PRO" else null) { designId = o.id }
                    }
                }

                key(refreshTick) { NeedsSection(design, params, city, onCity = { city = it }, onParam = ::set) }
                Spacer(Modifier.height(24.dp))
            }
            Button(
                onClick = { save(WidgetConfig(design.id, params), city.takeIf { design.needs == Needs.City }) },
                enabled = canSave(design, params, city),
                modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Done", fontWeight = FontWeight.SemiBold) }
        }
    }

    private fun canSave(design: WidgetDesign, params: Map<String, String>, city: String) = when (design.needs) {
        Needs.City -> city.isNotBlank()
        Needs.CountdownDate -> params[ParamKeys.DATE] != null
        else -> true
    }

    @Composable
    private fun NeedsSection(
        design: WidgetDesign, params: Map<String, String>, city: String,
        onCity: (String) -> Unit, onParam: (String, String) -> Unit,
    ) {
        val ctx = this
        var tick by remember { mutableIntStateOf(0) }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
        val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) lifecycleScope.launch { importPhoto(uri)?.let { onParam(ParamKeys.PHOTO, it) } }
        }
        key(tick) {
            when (design.needs) {
                Needs.Nothing -> Unit
                Needs.City -> {
                    SectionTitle("City")
                    OutlinedTextField(city, onCity, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Jaipur") },
                        supportingText = { Text("We look up weather by city, so Glint never asks for your location.") })
                }
                Needs.Calendar -> {
                    SectionTitle("Permission")
                    PermissionRow("Calendar", "Read your events to show them here. Nothing leaves your phone.",
                        AgendaRepository(ctx).hasPermission(), "Allow") { permission.launch(Manifest.permission.READ_CALENDAR) }
                }
                Needs.Steps -> {
                    SectionTitle("Permission")
                    val steps = StepsRepository(ctx)
                    if (!steps.hasSensor()) Text("This phone has no step counter, so the widget will show a dash.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else PermissionRow("Physical activity", "Count your steps with the phone's own sensor. No account, no cloud.",
                        steps.hasPermission(), "Allow") { permission.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
                }
                Needs.MediaAccess -> {
                    SectionTitle("Permission")
                    PermissionRow("Notification access", "Android only shares what's playing with apps that have this. Glint reads media only.",
                        NowPlaying.hasAccess(ctx), "Open settings") { startActivity(NowPlaying.accessIntent()) }
                }
                Needs.Text -> {
                    SectionTitle("Content")
                    OutlinedButton(onClick = {
                        startActivity(Intent(ctx, NoteEditorActivity::class.java)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            .putExtra(NoteEditorActivity.EXTRA_CHECKLIST, design.kind == WidgetKind.Checklist))
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (design.kind == WidgetKind.Checklist) "Edit checklist" else "Write note") }
                }
                Needs.CountdownDate -> {
                    SectionTitle("Countdown")
                    OutlinedTextField(params[ParamKeys.TITLE].orEmpty(), { onParam(ParamKeys.TITLE, it) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("e.g. Goa trip") })
                    Spacer(Modifier.height(8.dp))
                    val date = params[ParamKeys.DATE]
                    OutlinedButton(onClick = {
                        val d = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now().plusDays(30)
                        DatePickerDialog(ctx, { _, y, m, day -> onParam(ParamKeys.DATE, LocalDate.of(y, m + 1, day).toString()) },
                            d.year, d.monthValue - 1, d.dayOfMonth).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text(date ?: "Pick a date") }
                }
                Needs.PhotoPick -> {
                    SectionTitle("Photo")
                    Text("The system photo picker only gives Glint the one photo you choose.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth()) { Text(if (params[ParamKeys.PHOTO] != null) "Change photo" else "Pick a photo") }
                }
                Needs.Apps -> AppsPicker(max = if (design.id == "apps_grid") 8 else 4, csv = params[ParamKeys.APPS].orEmpty()) {
                    onParam(ParamKeys.APPS, it)
                }
                Needs.Zones -> ZonesPicker(max = if (design.id == "world_trio") 2 else 1, csv = params[ParamKeys.ZONES].orEmpty()) {
                    onParam(ParamKeys.ZONES, it)
                }
            }
        }
    }

    @Composable
    private fun AppsPicker(max: Int, csv: String, onChange: (String) -> Unit) {
        var apps by remember { mutableStateOf<List<PickableApp>>(emptyList()) }
        LaunchedEffect(Unit) { apps = loadLaunchableApps(this@WidgetConfigActivity) }
        val chosen = csv.split(',').filter { it.isNotBlank() }
        SectionTitle("Apps · ${chosen.size}/$max")
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxWidth().heightIn(max = 420.dp), userScrollEnabled = true) {
            items(apps, key = { it.pkg }) { app ->
                val on = app.pkg in chosen
                Column(
                    Modifier.padding(4.dp).clip16()
                        .border(1.5.dp, if (on) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable {
                            val next = if (on) chosen - app.pkg else if (chosen.size < max) chosen + app.pkg else chosen
                            onChange(next.joinToString(","))
                        }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(app.icon.asImageBitmap(), null, Modifier.size(40.dp))
                    Text(app.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    @Composable
    private fun ZonesPicker(max: Int, csv: String, onChange: (String) -> Unit) {
        val chosen = csv.split(',').filter { it.isNotBlank() }
        SectionTitle("Cities · ${chosen.size}/$max")
        CommonZones.forEach { (id, label) ->
            val on = id in chosen
            ChoiceCard(label, id.substringAfter('/').replace('_', ' '), on) {
                val next = if (on) chosen - id else if (chosen.size < max) chosen + id else chosen.drop(1) + id
                onChange(next.joinToString(","))
            }
        }
    }
}

private fun Modifier.clip16() = this.then(Modifier.background(Color.Transparent, RoundedCornerShape(16.dp)))
