package com.tlenparty.algolingo.kb.presentation

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tlenparty.algolingo.design.AlgoTheme
import kotlinx.coroutines.launch
import com.tlenparty.algolingo.core.markdown.parseBlocks
import com.tlenparty.algolingo.kb.domain.Note
import com.tlenparty.algolingo.kb.domain.findAnchorIndex

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
    // `#anchor` targets scroll within this note; a missing heading renders as plain text.
    val scope = rememberCoroutineScope()
    val local = { t: String -> if (t.startsWith("#")) findAnchorIndex(blocks, t.drop(1)) else null }
    val canOpen = { t: String -> if (t.startsWith("#")) local(t) != null else resolvable(t) }
    val open = { t: String, a: String? ->
        if (t.startsWith("#")) local(t)?.let { scope.launch { listState.animateScrollToItem(it) } } else onLink(t, a)
        Unit
    }
    Column(modifier.padding(horizontal = AlgoTheme.spacing.md)) {
        TextButton(onClick = onBack) { Text("Назад") }
        LazyColumn(state = listState) {
            itemsIndexed(blocks) { i, b -> BlockView(b, note.id, i, canOpen, open) }
        }
    }
}
