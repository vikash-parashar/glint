package app.glint.widgets.config

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import app.glint.widgets.engine.GlintWidget
import app.glint.widgets.store.ChecklistItem
import app.glint.widgets.store.NoteContent
import app.glint.widgets.store.NotesStore
import kotlinx.coroutines.launch

/** Edits the text of a Note widget, or the title and items of a Checklist widget. */
class NoteEditorActivity : ComponentActivity() {
    companion object { const val EXTRA_CHECKLIST = "checklist" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val isChecklist = intent.getBooleanExtra(EXTRA_CHECKLIST, false) ||
            GlintWidget.kindOf(this, id) == app.glint.widgets.catalog.WidgetKind.Checklist
        lifecycleScope.launch {
            val note = NotesStore(this@NoteEditorActivity).get(id)
            setContent { GlintSetupTheme { Editor(id, note, isChecklist) } }
        }
    }

    private fun save(id: Int, note: NoteContent) = lifecycleScope.launch {
        NotesStore(this@NoteEditorActivity).put(id, note)
        GlintWidget.refresh(this@NoteEditorActivity, id)
        finish()
    }

    @Composable
    private fun Editor(id: Int, initial: NoteContent, checklist: Boolean) {
        var text by remember { mutableStateOf(initial.text) }
        val items = remember { mutableStateListOf<ChecklistItem>().apply { addAll(initial.items) } }
        var draft by remember { mutableStateOf("") }
        val c = MaterialTheme.colorScheme

        Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding().imePadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(if (checklist) "Checklist" else "Note", color = c.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    text, { text = it }, modifier = Modifier.fillMaxWidth().let { if (checklist) it else it.heightIn(min = 220.dp) },
                    singleLine = checklist, placeholder = { Text(if (checklist) "Title, e.g. Groceries" else "Write anything…") },
                )
                if (checklist) {
                    Spacer(Modifier.height(16.dp))
                    items.forEachIndexed { i, item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).background(c.surface, RoundedCornerShape(12.dp)),
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(item.done, { items[i] = item.copy(done = it) })
                            Text(item.text, Modifier.weight(1f), color = if (item.done) c.onSurfaceVariant else c.onSurface,
                                textDecoration = if (item.done) TextDecoration.LineThrough else null)
                            TextButton(onClick = { items.removeAt(i) }) { Text("Remove") }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(draft, { draft = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("Add item") })
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(onClick = {
                            if (draft.isNotBlank()) { items.add(ChecklistItem(draft.trim())); draft = "" }
                        }) { Text("Add") }
                    }
                    if (items.any { it.done }) TextButton(onClick = { items.removeAll { it.done } }) { Text("Clear ticked items") }
                }
            }
            Button(
                onClick = {
                    val extra = draft.trim().takeIf { checklist && it.isNotEmpty() }?.let { listOf(ChecklistItem(it)) }.orEmpty()
                    save(id, NoteContent(text.trim(), items.toList() + extra))
                },
                modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp), shape = RoundedCornerShape(16.dp),
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }
        }
    }
}
