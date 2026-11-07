package com.tlenparty.algolingo.design

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private val kotlinKeywords = setOf(
    "fun", "val", "var", "if", "else", "for", "in", "while", "return", "when", "class", "object", "is",
    "null", "true", "false", "import", "package", "do", "break", "continue", "private", "override",
    "data", "interface", "suspend", "throw", "try", "catch", "finally", "const", "lateinit", "companion", "enum",
)

private val pythonKeywords = setOf(
    "def", "return", "in", "not", "and", "or", "None", "True", "False", "for", "while", "if", "elif", "else",
    "lambda", "class", "import", "from", "pass", "break", "continue", "is", "yield", "with", "as",
)

// SQL is case-insensitive: matched against the upper-cased identifier.
private val sqlKeywords = setOf(
    "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "ON", "GROUP", "BY", "HAVING",
    "ORDER", "ASC", "DESC", "LIMIT", "OFFSET", "AS", "AND", "OR", "NOT", "NULL", "IS", "IN", "EXISTS", "BETWEEN", "LIKE",
    "DISTINCT", "COUNT", "SUM", "AVG", "MIN", "MAX", "CASE", "WHEN", "THEN", "ELSE", "END", "INSERT", "INTO", "VALUES",
    "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "INDEX", "PRIMARY", "KEY", "REFERENCES", "UNIQUE", "ALTER", "DROP",
    "BEGIN", "COMMIT", "ROLLBACK", "UNION", "ALL", "WITH", "RETURNING", "TRUE", "FALSE", "DEFAULT", "CONSTRAINT",
)

private val yamlKeywords = setOf("true", "false", "null", "yes", "no", "on", "off")

private val httpKeywords = setOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "HTTP")

// Strings, numbers and identifiers are shared; comment syntax and extra tokens differ per language.
private const val REST = """|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|\b\d+(?:\.\d+)?[LlFf]?\b|\b[A-Za-z_]\w*\b"""

// A key is `name` right before `:` at the start of a line (after indent or `- `): YAML keys, HTTP header names.
// Only the named group is colored, so the indent stays untouched. Needs MULTILINE for `^`.
private const val KEY = """|^[ \t]*(?:- )?(?<key>[A-Za-z_][\w.-]*)(?=:)"""

// Alternation order gives precedence: comment and string match before keywords/numbers inside them.
// Kotlin: `@Annotation` is one token, colored as a keyword (Spring code is mostly annotations).
private val kotlinRe = Regex("""//[^\n]*|@[A-Za-z_]\w*$REST""")
private val pythonRe = Regex("""#[^\n]*$REST""")
private val sqlRe = Regex("""--[^\n]*$REST""")
private val yamlRe = Regex("""#[^\n]*$KEY$REST""", RegexOption.MULTILINE)
private val httpRe = Regex("""#[^\n]*$KEY$REST""", RegexOption.MULTILINE)

private class Rules(val tokenRe: Regex, val keywords: Set<String>, val ignoreCase: Boolean = false)

private val rules = mapOf(
    SyntaxLang.Kotlin to Rules(kotlinRe, kotlinKeywords),
    SyntaxLang.Python to Rules(pythonRe, pythonKeywords),
    SyntaxLang.Sql to Rules(sqlRe, sqlKeywords, ignoreCase = true),
    SyntaxLang.Yaml to Rules(yamlRe, yamlKeywords),
    SyntaxLang.Http to Rules(httpRe, httpKeywords),
)

fun highlight(code: String, colors: AlgoColors, lang: SyntaxLang = SyntaxLang.Kotlin): AnnotatedString = buildAnnotatedString {
    val r = rules.getValue(lang)
    append(code)
    for (m in r.tokenRe.findAll(code)) {
        val key = if (lang == SyntaxLang.Yaml || lang == SyntaxLang.Http) m.groups["key"] else null
        if (key != null) {
            addStyle(SpanStyle(color = colors.keyword), key.range.first, key.range.last + 1)
            continue
        }
        val v = m.value
        val c = when {
            v.startsWith("//") || v.startsWith("#") || v.startsWith("--") -> colors.comment
            v[0] == '"' || v[0] == '\'' -> colors.string
            v[0] == '@' -> colors.keyword
            v[0].isDigit() -> colors.number
            (if (r.ignoreCase) v.uppercase() else v) in r.keywords -> colors.keyword
            else -> null
        } ?: continue
        addStyle(SpanStyle(color = c), m.range.first, m.range.last + 1)
    }
}
