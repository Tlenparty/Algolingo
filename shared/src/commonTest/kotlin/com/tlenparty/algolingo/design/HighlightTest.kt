package com.tlenparty.algolingo.design

import com.tlenparty.algolingo.core.domain.CodeLang
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
        val s = highlight("def f(): return None  # c", colors, SyntaxLang.Python)
        assertEquals(listOf("def", "return", "None"), s.with(colors.keyword))
        assertEquals(listOf("# c"), s.with(colors.comment))
    }

    @Test fun pythonDoubleSlashIsNotComment() {
        assertEquals(emptyList(), highlight("a // b", colors, SyntaxLang.Python).with(colors.comment))
    }

    @Test fun kotlinDoubleSlashIsComment() {
        assertEquals(listOf("// c"), highlight("x = 1  // c", colors, SyntaxLang.Kotlin).with(colors.comment))
        assertEquals(emptyList(), highlight("x = 1  # c", colors, SyntaxLang.Kotlin).with(colors.comment))
    }

    @Test fun defaultIsKotlin() {
        val code = "fun f() = 1 // x"
        assertEquals(highlight(code, colors, SyntaxLang.Kotlin), highlight(code, colors))
        assertEquals(listOf("fun"), highlight(code, colors).with(colors.keyword))
    }

    @Test fun longLineFinishes() {
        val code = "a = 1  ".repeat(715).take(5000)
        assertEquals(5000, code.length)
        for (lang in SyntaxLang.entries) assertTrue(highlight(code, colors, lang).spanStyles.isNotEmpty())
    }

    @Test fun stringsAndNumbersBothLangs() {
        for (lang in SyntaxLang.entries) {
            val s = highlight("x = \"a\" + 'b' + 42", colors, lang)
            assertEquals(listOf("\"a\"", "'b'"), s.with(colors.string), "$lang")
            assertEquals(listOf("42"), s.with(colors.number), "$lang")
        }
        val py = highlight("s = \"# not\"", colors, SyntaxLang.Python)
        assertEquals(listOf("\"# not\""), py.with(colors.string))
        assertEquals(emptyList(), py.with(colors.comment))
    }

    @Test fun sqlKeywordsAnyCaseAndDashComment() {
        val s = highlight("select name FROM users -- всё\nWHERE city = 'Москва' LIMIT 10", colors, SyntaxLang.Sql)
        assertEquals(listOf("select", "FROM", "WHERE", "LIMIT"), s.with(colors.keyword))
        assertEquals(listOf("-- всё"), s.with(colors.comment))
        assertEquals(listOf("'Москва'"), s.with(colors.string))
        assertEquals(listOf("10"), s.with(colors.number))
    }

    @Test fun yamlKeysAndLiterals() {
        val s = highlight("spring:\n  datasource:\n    url: jdbc:h2:mem # c\n  - name: x\nenabled: true\nport: 8080", colors, SyntaxLang.Yaml)
        assertEquals(listOf("spring", "datasource", "url", "name", "enabled", "true", "port"), s.with(colors.keyword))
        assertEquals(listOf("# c"), s.with(colors.comment))
        assertEquals(listOf("8080"), s.with(colors.number))
    }

    @Test fun httpMethodsHeadersAndStatus() {
        val s = highlight("GET /users?limit=20 HTTP/1.1\nContent-Type: application/json\n\n201 Created", colors, SyntaxLang.Http)
        assertEquals(listOf("GET", "HTTP", "Content-Type"), s.with(colors.keyword))
        assertEquals(listOf("20", "1.1", "201"), s.with(colors.number))
    }

    @Test fun kotlinAnnotationsAreKeywords() {
        val s = highlight("@GetMapping(\"/{id}\")\nfun byId(@PathVariable id: Long)", colors, SyntaxLang.Kotlin)
        assertEquals(listOf("@GetMapping", "fun", "@PathVariable"), s.with(colors.keyword))
        assertEquals(listOf("\"/{id}\""), s.with(colors.string))
    }

    @Test fun ids() {
        assertEquals(listOf("kotlin", "python"), CodeLang.entries.map { it.id })
    }
}
