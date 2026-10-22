package com.tlenparty.algolingo.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Loads the lesson once (survives rotation without a reload/blank frame). null = loading, failure = error screen. */
class LessonViewModel(
    id: String,
    seed: Int,
    private val onFinish: (LessonResult) -> Unit = {},
    load: suspend (String) -> Result<Lesson> = ::loadLesson,
) : ViewModel() {
    private val _state = MutableStateFlow<Result<LessonState>?>(null)
    val state: StateFlow<Result<LessonState>?> = _state

    init {
        viewModelScope.launch { _state.value = load(id).map { LessonReducer.start(it, seed) } }
    }

    /** onFinish fires once, on the transition into Done; the VM survives rotation, so no double write. */
    fun on(a: LessonAction) {
        val prev = _state.value?.getOrNull() ?: return
        val next = LessonReducer.reduce(prev, a)
        _state.value = Result.success(next)
        if (prev.phase !is Phase.Done && next.phase is Phase.Done) onFinish(LessonReducer.result(next))
    }
}
