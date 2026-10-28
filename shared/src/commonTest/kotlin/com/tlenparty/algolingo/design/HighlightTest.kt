package com.tlenparty.algolingo.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import kotlin.test.*

class HighlightTest {
    private val n = Color.Black
    private val colors = AlgoColors(
        n, n, n, n, n, n, n, n,
        keyword = Color.Red, number = Color.Green, string = Color.Blue, comment = Color.Gray,
        greenLedge = n, redFill = n, redLedge = n, amber = n, amberLedge = n, focus = n,
        disabledFace = n, disabledLabel = n, secondaryText = n, xpText = n,
    )

    /** Text of every span with the given color. */
    private fun AnnotatedString.with(c: Color) =
        spanStyles.filter { it.item.color == c }.map { text.substring(it.start, it.end) }

    @Test fun pythonKeywordsAndHashComment() {
        val s = highlight("def f(): return None  # c", colors, CodeLang.Python)
        assertEquals(listOf("def", "return", "None"), s.with(colors.keyword))
        assertEquals(listOf("# c"), s.with(colors.comment))
    }

    @Test fun pythonDoubleSlashIsNotComment() {
        assertEquals(emptyList(), highlight("a // b", colors, CodeLang.Python).with(colors.comment))
    }

    @Test fun kotlinDoubleSlashIsComment() {
        assertEquals(listOf("// c"), highlight("x = 1  // c", colors, CodeLang.Kotlin).with(colors.comment))
        assertEquals(emptyList(), highlight("x = 1  # c", colors, CodeLang.Kotlin).with(colors.comment))
    }

    @Test fun defaultIsKotlin() {
        val code = "fun f() = 1 // x"
        assertEquals(highlight(code, colors, CodeLang.Kotlin), highlight(code, colors))
        assertEquals(listOf("fun"), highlight(code, colors).with(colors.keyword))
    }

    @Test fun longLineFinishes() {
        val code = "a = 1  ".repeat(715).take(5000)
        assertEquals(5000, code.length)
        for (lang in CodeLang.entries) assertTrue(highlight(code, colors, lang).spanStyles.isNotEmpty())
    }

    @Test fun stringsAndNumbersBothLangs() {
        for (lang in CodeLang.entries) {
            val s = highlight("x = \"a\" + 'b' + 42", colors, lang)
            assertEquals(listOf("\"a\"", "'b'"), s.with(colors.string), "$lang")
            assertEquals(listOf("42"), s.with(colors.number), "$lang")
        }
        val py = highlight("s = \"# not\"", colors, CodeLang.Python)
        assertEquals(listOf("\"# not\""), py.with(colors.string))
        assertEquals(emptyList(), py.with(colors.comment))
    }

    @Test fun ids() {
        assertEquals(listOf("kotlin", "python"), CodeLang.entries.map { it.id })
    }
}
