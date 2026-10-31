package com.tlenparty.algolingo.lesson.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.lesson.domain.LoadLessonUseCase
import com.tlenparty.algolingo.progress.domain.RecordLessonResultUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Loading and failure of the lesson screen; the lesson itself is the [LessonReducer] state machine. */
sealed interface LessonUiState {
    data object Loading : LessonUiState
    data object Error : LessonUiState
    data class Content(val state: LessonState) : LessonUiState
}

/** Loads the lesson once (survives rotation without a reload/blank frame). */
class LessonViewModel(
    private val id: String,
    seed: Int,
    review: Boolean = false,
    /** Fixed at creation: switching language on Home does not reload an open lesson. */
    lang: CodeLang = CodeLang.Kotlin,
    loadLesson: LoadLessonUseCase,
    private val recordResult: RecordLessonResultUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val state: StateFlow<LessonUiState> = _state

    init {
        viewModelScope.launch {
            val start = loadLesson(id, lang, review).getOrNull()?.let { LessonReducer.start(it, seed) }
            _state.value = start?.let { LessonUiState.Content(it) } ?: LessonUiState.Error
            // A review strips theory, so a lesson with no playable exercise starts in Done: record it like normal mode does, or it stays due forever.
            if (start?.phase is Phase.Done) recordResult(id, LessonReducer.result(start))
        }
    }

    /** The result is recorded once, on the transition into Done (or at load if the lesson starts there); the VM survives rotation, so no double write. */
    fun on(a: LessonAction) {
        val prev = (_state.value as? LessonUiState.Content)?.state ?: return
        val next = LessonReducer.reduce(prev, a)
        _state.value = LessonUiState.Content(next)
        if (prev.phase !is Phase.Done && next.phase is Phase.Done) recordResult(id, LessonReducer.result(next))
    }
}
