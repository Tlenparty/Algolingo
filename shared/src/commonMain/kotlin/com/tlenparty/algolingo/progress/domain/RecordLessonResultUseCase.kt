package com.tlenparty.algolingo.progress.domain

import com.tlenparty.algolingo.lesson.domain.LessonResult
import kotlinx.coroutines.Job

class RecordLessonResultUseCase(private val progress: ProgressRepository) {
    /** today() is read inside the transform: the day of the write, not of the lesson start. */
    operator fun invoke(id: String, result: LessonResult): Job =
        progress.update { applyLessonResult(it, id, result, progress.today()) }
}
