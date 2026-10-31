package com.tlenparty.algolingo.progress.domain

import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface ProgressRepository {
    /** null = not loaded yet. An IO error shows zeros instead of crashing Home. */
    val progress: StateFlow<Progress?>

    /** True once a write failed even after a retry, so Home can say the lesson was not saved. The next good write clears it. */
    val saveFailed: StateFlow<Boolean>

    fun today(): LocalDate

    /** Runs in an app-lifetime scope: leaving a screen must not cancel a write. */
    fun update(transform: (Progress) -> Progress): Job

    fun setLang(lang: CodeLang): Job
}
