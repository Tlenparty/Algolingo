package com.tlenparty.algolingo.kb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KbSearchTest {
    private val notes = listOf(
        Note("a", "Заметка", "Темы", "здесь про бинарный поиск и (скобки) [x] .*"),
        Note("b", "Бинарный поиск", "Темы", "текст"),
    )

    @Test fun blankIsEmpty() = assertTrue(searchNotes(notes, "  ").isEmpty())

    @Test fun titleBeforeBodyAndCyrillicCase() {
        val r = searchNotes(notes, "бинарный")
        assertEquals(listOf("b", "a"), r.map { it.note.id })
        assertEquals("", r[0].snippet)
        assertTrue(r[1].snippet.contains("бинарный"))
    }

    @Test fun metacharactersLiteral() {
        for (q in listOf("(", "[", ".*")) assertEquals(listOf("a"), searchNotes(notes, q).map { it.note.id })
    }

    @Test fun titleThenBodySnippetKeepsOriginalCase() {
        val ns = listOf(
            Note("x", "Заметка", "Темы", "Тут живёт Корутина и её скоуп"),
            Note("y", "корутина: основы", "Темы", "текст"),
        )
        val r = searchNotes(ns, "корутина")
        assertEquals(listOf("y", "x"), r.map { it.note.id })
        assertTrue(r[1].snippet.contains("Корутина"))
    }

    @Test fun lowercaseLengthChangeDoesNotCrash() {
        // "İ".lowercase() is 2 chars: the lowerRaw index runs past raw.
        val hits = searchNotes(listOf(Note("z", "Заметка", "Темы", "İ".repeat(40) + " цель")), "цель")
        assertEquals(listOf("z"), hits.map { it.note.id })
    }
}
