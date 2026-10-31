package com.tlenparty.algolingo.lesson.domain

import com.tlenparty.algolingo.core.domain.CodeLang

class LoadLessonUseCase(private val lessons: LessonRepository) {
    /** [review] = due replay: same player, theory skipped (PROG-05). */
    suspend operator fun invoke(id: String, lang: CodeLang, review: Boolean): Result<Lesson> =
        lessons.load(id, lang).map { if (review) it.copy(theory = emptyList()) else it }
}
