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
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.kb.Note

@Composable
fun KbListScreen(notes: List<Note>, onOpen: (Note) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier) {
        notes.groupBy { it.section }.forEach { (section, items) ->
            item(key = "h-$section") {
                Text(
                    section,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                )
            }
            items(items, key = { it.id }) { note ->
                Text(
                    note.title,
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(note) }.padding(16.dp, 12.dp),
                )
            }
        }
    }
}
