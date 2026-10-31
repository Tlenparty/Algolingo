package com.tlenparty.algolingo.kb.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tlenparty.algolingo.kb.domain.Note

private const val KB_ERROR = "Не удалось загрузить базу знаний"

@Composable
fun KbListHost(vm: KbListViewModel, onOpen: (Note) -> Unit) {
    // KB loads lazily on first open, so a bad KB file never blocks Home or lessons.
    LaunchedEffect(Unit) { vm.on(KbListAction.Load) }
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        KbListUiState.Loading -> Text("…")
        KbListUiState.Error -> Text(KB_ERROR)
        is KbListUiState.Content -> KbListScreen(s.notes, s.search, onOpen, onQuery = { vm.on(KbListAction.QueryChanged(it)) })
    }
}

@Composable
fun NoteHost(vm: NoteViewModel, images: KbImages, anchor: String?, onOpenNote: (NoteEffect.OpenNote) -> Unit, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.on(NoteAction.Load) }
    val open by rememberUpdatedState(onOpenNote)
    LaunchedEffect(vm) { vm.effects.collect { if (it is NoteEffect.OpenNote) open(it) } }
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        NoteUiState.Loading -> Text("…")
        NoteUiState.Error -> Text(KB_ERROR)
        NoteUiState.NotFound -> Text("Заметка не найдена")
        is NoteUiState.Content -> CompositionLocalProvider(LocalKbImages provides images) {
            NoteScreen(
                s.note, s.blocks, anchor,
                resolvable = vm::canOpen,
                onLink = { t, a -> vm.on(NoteAction.FollowLink(t, a)) },
                onBack = onBack,
            )
        }
    }
}
