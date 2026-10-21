package com.tlenparty.algolingo.design

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private val keywords = setOf(
    "fun", "val", "var", "if", "else", "for", "in", "while", "return", "when", "class", "object", "is",
    "null", "true", "false", "import", "package", "do", "break", "continue", "private", "override",
    "data", "interface",
)

// Alternation order gives precedence: comment and string match before keywords/numbers inside them.
private val tokenRe = Regex("""//[^\n]*|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|\b\d+(?:\.\d+)?[LlFf]?\b|\b[A-Za-z_]\w*\b""")

fun highlight(code: String, colors: AlgoColors): AnnotatedString = buildAnnotatedString {
    append(code)
    for (m in tokenRe.findAll(code)) {
        val v = m.value
        val c = when {
            v.startsWith("//") -> colors.comment
            v[0] == '"' || v[0] == '\'' -> colors.string
            v[0].isDigit() -> colors.number
            v in keywords -> colors.keyword
            else -> null
        } ?: continue
        addStyle(SpanStyle(color = c), m.range.first, m.range.last + 1)
    }
}
