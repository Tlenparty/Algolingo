package com.tlenparty.algolingo.kb.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.kb.Note
import com.tlenparty.algolingo.kb.findAnchorIndex
import com.tlenparty.algolingo.kb.parseBlocks

@Composable
fun NoteScreen(
    note: Note,
    anchor: String?,
    resolvable: (String) -> Boolean,
    onLink: (String, String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(note.raw) { parseBlocks(note.raw) }
    val listState = rememberLazyListState()
    // Jump to the anchor once; on back-navigation/rotation the saved scroll position wins.
    var anchorDone by rememberSaveable(note.id, anchor) { mutableStateOf(false) }
    LaunchedEffect(note.id, anchor) {
        if (!anchorDone && anchor != null) findAnchorIndex(blocks, anchor)?.let { listState.scrollToItem(it) }
        anchorDone = true
    }
    Column(modifier.padding(horizontal = 16.dp)) {
        TextButton(onClick = onBack) { Text("Назад") }
        LazyColumn(state = listState) {
            itemsIndexed(blocks) { i, b -> BlockView(b, note.id, i, resolvable, onLink) }
        }
    }
}
