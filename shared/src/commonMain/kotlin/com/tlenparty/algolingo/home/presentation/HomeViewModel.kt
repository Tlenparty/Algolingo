package com.tlenparty.algolingo.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.home.domain.BuildHomeModelUseCase
import com.tlenparty.algolingo.home.domain.HomeModel
import com.tlenparty.algolingo.progress.domain.Catalog
import com.tlenparty.algolingo.progress.domain.CatalogRepository
import com.tlenparty.algolingo.progress.domain.ProgressRepository
import com.tlenparty.algolingo.progress.domain.Track
import com.tlenparty.algolingo.progress.domain.routeLang
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    /** Catalog or progress are still loading: plain background, no flicker of wrong statuses. */
    data object Loading : HomeUiState
    data class Content(val model: HomeModel, val lang: CodeLang, val saveFailed: Boolean) : HomeUiState
}

sealed interface HomeAction {
    data object RefreshDate : HomeAction
    data class StartLesson(val id: String) : HomeAction
    data object StartReview : HomeAction
    data class SelectLang(val lang: CodeLang) : HomeAction
}

sealed interface HomeEffect {
    data class OpenLesson(val id: String, val review: Boolean, val lang: CodeLang) : HomeEffect
}

class HomeViewModel(
    private val track: Track,
    private val progress: ProgressRepository,
    catalogs: CatalogRepository,
    private val buildHome: BuildHomeModelUseCase,
) : ViewModel() {
    private val catalog = MutableStateFlow<Catalog?>(null)
    private val today = MutableStateFlow(progress.today())
    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    val state: StateFlow<HomeUiState> = combine(catalog, progress.progress, today, progress.saveFailed) { c, p, day, saveFailed ->
        if (c == null || p == null) HomeUiState.Loading
        else HomeUiState.Content(buildHome(track, c, p, day), p.lang, saveFailed)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState.Loading)

    init {
        viewModelScope.launch { catalog.value = catalogs.catalog(track) }
    }

    fun on(a: HomeAction) {
        when (a) {
            // The process can outlive midnight: the screen asks to re-read the date while it is visible.
            HomeAction.RefreshDate -> today.value = progress.today()
            is HomeAction.StartLesson -> open(a.id, review = false)
            // ponytail: review opens only the first due lesson; chain them into one session if N regularly exceeds 3.
            HomeAction.StartReview -> (state.value as? HomeUiState.Content)?.model?.due?.firstOrNull()?.let { open(it, review = true) }
            is HomeAction.SelectLang -> if (track == Track.Code) progress.setLang(a.lang)
        }
    }

    private fun open(id: String, review: Boolean) {
        val c = catalog.value ?: return
        val lang = (state.value as? HomeUiState.Content)?.lang ?: return
        _effects.trySend(HomeEffect.OpenLesson(id, review, c.routeLang(id, track.langFilter(lang))))
    }
}
