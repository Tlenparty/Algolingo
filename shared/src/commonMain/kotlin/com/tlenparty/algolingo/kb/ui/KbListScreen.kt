package com.tlenparty.algolingo.kb.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.kb.Note
import com.tlenparty.algolingo.kb.SearchHit

@Composable
fun KbListScreen(
    notes: List<Note>,
    onOpen: (Note) -> Unit,
    modifier: Modifier = Modifier,
    search: (String) -> List<SearchHit> = { emptyList() },
) {
    var query by rememberSaveable { mutableStateOf("") }
    val hits = remember(query, notes) { search(query) }
    // Open section names; empty = all collapsed. List<String> is saveable on Android and iOS.
    var open by rememberSaveable { mutableStateOf(emptyList<String>()) }
    LazyColumn(modifier) {
        item(key = "search") {
            OutlinedTextField(
                query, { query = it },
                Modifier.fillMaxWidth().padding(AlgoTheme.spacing.md, AlgoTheme.spacing.sm),
                label = { Text("Поиск по заметкам") },
                singleLine = true,
            )
        }
        if (query.isNotBlank()) {
            if (hits.isEmpty()) item(key = "none") { Text("Ничего не найдено", Modifier.padding(AlgoTheme.spacing.md)) }
            items(hits, key = { "s-" + it.note.id }) { h ->
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
