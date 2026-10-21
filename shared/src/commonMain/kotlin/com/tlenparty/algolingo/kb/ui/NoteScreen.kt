package com.tlenparty.algolingo.kb.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.kb.Note
import com.tlenparty.algolingo.kb.parseBlocks

@Composable
fun NoteScreen(note: Note, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val blocks = remember(note.raw) { parseBlocks(note.raw) }
    Column(modifier.padding(horizontal = 16.dp)) {
        TextButton(onClick = onBack) { Text("Назад") }
        LazyColumn { items(blocks) { BlockView(it) } }
    }
}
