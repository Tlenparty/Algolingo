package com.tlenparty.algolingo.design

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private val kotlinKeywords = setOf(
    "fun", "val", "var", "if", "else", "for", "in", "while", "return", "when", "class", "object", "is",
    "null", "true", "false", "import", "package", "do", "break", "continue", "private", "override",
    "data", "interface",
)

private val pythonKeywords = setOf(
    "def", "return", "in", "not", "and", "or", "None", "True", "False", "for", "while", "if", "elif", "else",
    "lambda", "class", "import", "from", "pass", "break", "continue", "is", "yield", "with", "as",
)

// Strings, numbers and identifiers are shared; only the comment syntax differs per language.
private const val REST = """|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|\b\d+(?:\.\d+)?[LlFf]?\b|\b[A-Za-z_]\w*\b"""

// Alternation order gives precedence: comment and string match before keywords/numbers inside them.
private val kotlinRe = Regex("""//[^\n]*$REST""")
private val pythonRe = Regex("""#[^\n]*$REST""")

fun highlight(code: String, colors: AlgoColors, lang: CodeLang = CodeLang.Kotlin): AnnotatedString = buildAnnotatedString {
    val (tokenRe, keywords) = when (lang) {
        CodeLang.Kotlin -> kotlinRe to kotlinKeywords
        CodeLang.Python -> pythonRe to pythonKeywords
    }
    append(code)
    for (m in tokenRe.findAll(code)) {
        val v = m.value
        val c = when (v[0]) {
            '/', '#' -> colors.comment
            '"', '\'' -> colors.string
            else -> when {
                v[0].isDigit() -> colors.number
                v in keywords -> colors.keyword
                else -> null
            }
        } ?: continue
        addStyle(SpanStyle(color = c), m.range.first, m.range.last + 1)
    }
}
