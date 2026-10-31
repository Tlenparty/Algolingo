package com.tlenparty.algolingo.lesson.domain

import com.tlenparty.algolingo.core.domain.CodeLang

fun interface LessonRepository {
    /** Failure = the lesson file is missing or broken; never throws on bad content. */
    suspend fun load(id: String, lang: CodeLang): Result<Lesson>
}
