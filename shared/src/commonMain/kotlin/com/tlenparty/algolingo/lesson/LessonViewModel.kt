package com.tlenparty.algolingo.lesson

import androidx.lifecycle.ViewModel
import com.tlenparty.algolingo.design.CodeLang
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** review = due replay: same player, theory skipped (PROG-05). Loads the lesson once (survives rotation without a reload/blank frame). null = loading, failure = error screen. */
class LessonViewModel(
    id: String,
    seed: Int,
    review: Boolean = false,
    private val onFinish: (LessonResult) -> Unit = {},
    /** Fixed at creation: switching language on Home does not reload an open lesson. */
    lang: CodeLang = CodeLang.Kotlin,
    load: suspend (String, CodeLang) -> Result<Lesson> = ::loadLesson,
) : ViewModel() {
    private val _state = MutableStateFlow<Result<LessonState>?>(null)
    val state: StateFlow<Result<LessonState>?> = _state

    init {
        viewModelScope.launch {
            val r = load(id, lang).map { LessonReducer.start(if (review) it.copy(theory = emptyList()) else it, seed) }
            _state.value = r
            // A review strips theory, so a lesson with no playable exercise starts in Done: record it like normal mode does, or it stays due forever.
            r.getOrNull()?.takeIf { it.phase is Phase.Done }?.let { onFinish(LessonReducer.result(it)) }
        }
    }

    /** onFinish fires once, on the transition into Done (or at load if the lesson starts there); the VM survives rotation, so no double write. */
    fun on(a: LessonAction) {
        val prev = _state.value?.getOrNull() ?: return
        val next = LessonReducer.reduce(prev, a)
        _state.value = Result.success(next)
        if (prev.phase !is Phase.Done && next.phase is Phase.Done) onFinish(LessonReducer.result(next))
    }
}
