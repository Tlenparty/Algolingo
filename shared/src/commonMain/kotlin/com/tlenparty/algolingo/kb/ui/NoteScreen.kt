package com.tlenparty.algolingo.kb.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.kb.Note

@Composable
fun NoteScreen(note: Note, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp)) {
        TextButton(onClick = onBack) { Text("Назад") }
        Text(note.title, style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.verticalScroll(rememberScrollState())) { Text(note.raw) }
    }
}
