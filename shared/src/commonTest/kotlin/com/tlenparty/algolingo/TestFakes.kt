package com.tlenparty.algolingo

import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.progress.domain.Progress
import com.tlenparty.algolingo.progress.domain.ProgressRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/** In-memory progress for ViewModel and use case tests: writes apply at once. */
class FakeProgressRepository(start: Progress? = Progress(), var day: LocalDate = LocalDate(2026, 10, 31)) : ProgressRepository {
    override val progress = MutableStateFlow(start)
    override val saveFailed = MutableStateFlow(false)
    var writes = 0
        private set

    override fun today() = day

    override fun update(transform: (Progress) -> Progress): Job {
        writes++
        progress.value = transform(progress.value ?: Progress())
        return Job().apply { complete() }
    }

    override fun setLang(lang: CodeLang) = update { it.copy(lang = lang) }
}
