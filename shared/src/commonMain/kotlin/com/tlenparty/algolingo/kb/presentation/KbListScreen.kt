package com.tlenparty.algolingo.kb.presentation

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.kb.domain.Note

@Composable
fun KbListScreen(
    notes: List<Note>,
    search: KbSearch?,
    onOpen: (Note) -> Unit,
    modifier: Modifier = Modifier,
    onQuery: (String) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    // Restored after rotation or a tab switch: search again so the results match the text.
    LaunchedEffect(Unit) { if (query.isNotBlank()) onQuery(query) }
    // Open section names; empty = all collapsed. List<String> is saveable on Android and iOS.
    var open by rememberSaveable { mutableStateOf(emptyList<String>()) }
    LazyColumn(modifier) {
        item(key = "search") {
            OutlinedTextField(
                query, { query = it; onQuery(it) },
                Modifier.fillMaxWidth().padding(AlgoTheme.spacing.md, AlgoTheme.spacing.sm),
                label = { Text("Поиск по заметкам") },
                singleLine = true,
            )
        }
        if (query.isNotBlank()) {
            val found = search?.hits.orEmpty()
            // only a finished search for exactly this query may say "nothing found"
            if (search?.query == query && found.isEmpty()) item(key = "none") { Text("Ничего не найдено", Modifier.padding(AlgoTheme.spacing.md)) }
            items(found, key = { "s-" + it.note.id }) { h ->
                Column(Modifier.fillMaxWidth().clickable { onOpen(h.note) }.padding(AlgoTheme.spacing.md, AlgoTheme.spacing.smd)) {
                    Text(h.note.title)
                    if (h.snippet.isNotEmpty()) {
                        Text(h.snippet, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            return@LazyColumn
        }
        notes.groupBy { it.section }.forEach { (section, items) ->
            val isOpen = section in open
            item(key = "h-$section") {
                Text(
                    "${if (isOpen) "▾ " else "▸ "}$section (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { open = if (section in open) open - section else open + section }
                        .padding(AlgoTheme.spacing.md, AlgoTheme.spacing.md, AlgoTheme.spacing.md, AlgoTheme.spacing.sm),
                )
            }
            if (isOpen) items(items, key = { it.id }) { note ->
                Text(
                    note.title,
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(note) }.padding(AlgoTheme.spacing.md, AlgoTheme.spacing.smd),
                )
            }
        }
    }
}
