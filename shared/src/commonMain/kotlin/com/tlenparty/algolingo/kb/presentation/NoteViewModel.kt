package com.tlenparty.algolingo.kb.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlenparty.algolingo.core.markdown.Block
import com.tlenparty.algolingo.core.markdown.parseBlocks
import com.tlenparty.algolingo.kb.domain.KbRepository
import com.tlenparty.algolingo.kb.domain.Note
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface NoteUiState {
    data object Loading : NoteUiState
    data object Error : NoteUiState
    data object NotFound : NoteUiState
    data class Content(val note: Note, val blocks: List<Block>) : NoteUiState
}

sealed interface NoteAction {
    /** NoteRoute triggers its own load: Activity recreation restores it before the KB list was ever opened. */
    data object Load : NoteAction
    data class FollowLink(val target: String, val anchor: String?) : NoteAction
}

sealed interface NoteEffect {
    data class OpenNote(val id: String, val anchor: String?) : NoteEffect
}

class NoteViewModel(private val id: String, private val repo: KbRepository) : ViewModel() {
    private val _state = MutableStateFlow<NoteUiState>(NoteUiState.Loading)
    val state: StateFlow<NoteUiState> = _state
    private val _effects = Channel<NoteEffect>(Channel.BUFFERED)
    val effects: Flow<NoteEffect> = _effects.receiveAsFlow()

    fun canOpen(target: String): Boolean = repo.resolve(target) != null

    fun on(a: NoteAction) {
        when (a) {
            NoteAction.Load -> load()
            is NoteAction.FollowLink -> repo.resolve(a.target)?.let { _effects.trySend(NoteEffect.OpenNote(it.id, a.anchor)) }
        }
    }

    private fun load() {
        if (_state.value is NoteUiState.Content) return
        viewModelScope.launch {
            _state.value = when {
                repo.load().isFailure -> NoteUiState.Error
                else -> repo.byId(id)?.let { NoteUiState.Content(it, parseBlocks(it.raw)) } ?: NoteUiState.NotFound
            }
        }
    }
}
