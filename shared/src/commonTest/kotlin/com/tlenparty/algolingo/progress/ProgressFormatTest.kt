package com.tlenparty.algolingo.progress

import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.progress.data.decodeProgress
import com.tlenparty.algolingo.progress.data.encodeProgress
import com.tlenparty.algolingo.progress.domain.LessonProgress
import com.tlenparty.algolingo.progress.domain.Progress
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** progress.json lives on users' phones: this exact text must keep working in every build. */
class ProgressFormatTest {
    private val golden = """{"schemaVersion":1,"xp":15,"streak":2,"lastActiveDay":"2026-10-30",""" +
        """"lessons":{"sd0.3":{"step":1,"due":"2026-11-02"},"l0.1":{"step":3,"due":null}},"lang":"python"}"""
    private val progress = Progress(
        xp = 15, streak = 2, lastActiveDay = LocalDate(2026, 10, 30),
        lessons = mapOf("sd0.3" to LessonProgress(1, LocalDate(2026, 11, 2)), "l0.1" to LessonProgress(3, null)),
        lang = CodeLang.Python,
    )

    @Test fun writesExactFormat() = assertEquals(golden, encodeProgress(progress))

    @Test fun readsExactFormat() = assertEquals(progress, decodeProgress(golden))
}
