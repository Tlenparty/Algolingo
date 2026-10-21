package com.tlenparty.algolingo.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Loads the lesson once (survives rotation without a reload/blank frame). null = loading, failure = error screen. */
class LessonViewModel(id: String, seed: Int, load: suspend (String) -> Result<Lesson> = ::loadLesson) : ViewModel() {
    private val _state = MutableStateFlow<Result<LessonState>?>(null)
    val state: StateFlow<Result<LessonState>?> = _state

    init {
        viewModelScope.launch { _state.value = load(id).map { LessonReducer.start(it, seed) } }
    }

    fun on(a: LessonAction) {
        _state.update { r -> r?.map { LessonReducer.reduce(it, a) } }
    }
}
