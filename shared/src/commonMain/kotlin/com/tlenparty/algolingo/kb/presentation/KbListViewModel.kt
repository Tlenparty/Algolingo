package com.tlenparty.algolingo.kb.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlenparty.algolingo.kb.domain.KbRepository
import com.tlenparty.algolingo.kb.domain.Note
import com.tlenparty.algolingo.kb.domain.SearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The last finished search, kept while the next one runs so typing does not blank the list. */
data class KbSearch(val query: String, val hits: List<SearchHit>)

sealed interface KbListUiState {
    data object Loading : KbListUiState
    data object Error : KbListUiState
    data class Content(val notes: List<Note>, val search: KbSearch? = null) : KbListUiState
}

sealed interface KbListAction {
    /** Sent each time the screen appears: a failed load is not cached, so this retries it. */
    data object Load : KbListAction
    data class QueryChanged(val query: String) : KbListAction
}

class KbListViewModel(private val repo: KbRepository) : ViewModel() {
    private val _state = MutableStateFlow<KbListUiState>(KbListUiState.Loading)
    val state: StateFlow<KbListUiState> = _state
    private var searchJob: Job? = null

    fun on(a: KbListAction) {
        when (a) {
            KbListAction.Load -> load()
            is KbListAction.QueryChanged -> search(a.query)
        }
    }

    private fun load() {
        if (_state.value is KbListUiState.Content) return
        viewModelScope.launch {
            _state.value = repo.load().fold({ KbListUiState.Content(it) }, { KbListUiState.Error })
        }
    }

    // Full-text scan of every note is too slow for the UI thread (monkey ANR in 07.1-06): debounce, then
    // search on Default; a new query cancels the pending one.
    private fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            setSearch(null)
            return
        }
        searchJob = viewModelScope.launch {
            delay(150)
            setSearch(KbSearch(query, withContext(Dispatchers.Default) { repo.search(query) }))
        }
    }

    private fun setSearch(search: KbSearch?) {
        (_state.value as? KbListUiState.Content)?.let { _state.value = it.copy(search = search) }
    }
}
