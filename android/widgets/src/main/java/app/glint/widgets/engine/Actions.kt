package app.glint.widgets.engine

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import app.glint.data.NowPlaying
import app.glint.focus.FocusController
import app.glint.widgets.catalog.WidgetKind
import app.glint.widgets.store.NotesStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

object ActionKeys {
    val Command = ActionParameters.Key<String>("cmd")
    val Index = ActionParameters.Key<Int>("index")
}

class MusicAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val cmd = parameters[ActionKeys.Command]?.let { runCatching { NowPlaying.Command.valueOf(it) }.getOrNull() } ?: return
        NowPlaying.send(context, cmd)
        delay(400) // let the player update its state before we redraw
        GlintWidget.refreshKinds(context, setOf(WidgetKind.Music))
    }
}

class ToggleChecklistItem : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val index = parameters[ActionKeys.Index] ?: return
        val id = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        NotesStore(context).toggle(id, index)
        GlintWidget().update(context, glanceId)
    }
}

/** Starts a 25-minute session with the user's usual allowed apps. */
class StartFocus : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val c = FocusController(context)
        if (c.repo.activeNow() == null) c.start("Deep work", 25, c.repo.defaultAllowed.first(), silence = true)
        GlintWidget.refreshKinds(context, setOf(WidgetKind.Focus))
    }
}
