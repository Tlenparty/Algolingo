package com.tlenparty.algolingo.lesson

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class LessonViewModel(lesson: Lesson, seed: Int) : ViewModel() {
    private val _state = MutableStateFlow(LessonReducer.start(lesson, seed))
    val state: StateFlow<LessonState> = _state

    fun on(a: LessonAction) {
        _state.update { LessonReducer.reduce(it, a) }
    }
}
