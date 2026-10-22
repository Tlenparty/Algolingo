package com.tlenparty.algolingo.kb

sealed interface Block
data class Heading(val level: Int, val text: String) : Block
data class Paragraph(val text: String) : Block
data class ListItem(val ordered: Boolean, val number: Int, val depth: Int, val text: String) : Block
data class Code(val lang: String, val text: String) : Block
data class Table(val header: List<String>, val rows: List<List<String>>) : Block
data class Quote(val blocks: List<Block>) : Block
data class Callout(
    val type: String,
    val foldable: Boolean,
    val collapsed: Boolean,
    val title: String,
    val blocks: List<Block>,
) : Block
object Rule : Block
/** Whole-line `![[file.png]]` embed; [file] is a name under `files/kb/img/`. */
data class ImageBlock(val file: String) : Block

sealed interface Inline
data class Text(val v: String) : Inline
data class Bold(val v: String) : Inline
data class Italic(val v: String) : Inline
data class InlineCode(val v: String) : Inline
/** `==highlight==`; [v] is raw markdown, parsed again when rendered. */
data class Mark(val v: String) : Inline
/** [target] is a note title, or `#anchor` for a link within the current note. */
data class Link(val target: String, val anchor: String?, val label: String) : Inline

private val headingRe = Regex("^(#{1,6})\\s+(.*)$")
private val listRe = Regex("^(\\s*)([-*]|(\\d+)\\.)\\s+(.*)$")
private val alignRe = Regex("^\\|?[\\s:|-]+$")
private val calloutRe = Regex("^\\[!([A-Za-z]+)]([+-]?)\\s*(.*)$")
private val imageRe = Regex("^!\\[\\[([^\\]|]+\\.(?:png|jpe?g))(?:\\|[^\\]]*)?]]$")

private fun isTableStart(lines: List<String>, i: Int) =
    lines[i].trimStart().startsWith("|") && i + 1 < lines.size &&
        lines[i + 1].contains('-') && alignRe.matches(lines[i + 1].trim())

private fun startsBlock(lines: List<String>, i: Int): Boolean {
    val t = lines[i].trim()
    return t.isEmpty() || t.startsWith("```") || t.startsWith(">") || t == "---" ||
        headingRe.matches(t) || listRe.matches(lines[i]) || isTableStart(lines, i) || imageRe.matches(t)
}

fun parseBlocks(src: String): List<Block> {
    val lines = src.replace("\r\n", "\n").split('\n')
    val out = ArrayList<Block>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val t = line.trim()
        when {
            t.isEmpty() -> i++
            t.startsWith("```") -> {
                val body = ArrayList<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) body.add(lines[i++])
                i++ // closing fence (or past end when unterminated)
                out.add(Code(t.drop(3).trim(), body.joinToString("\n")))
            }
            t.startsWith(">") -> {
                val body = ArrayList<String>()
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    body.add(lines[i].trimStart().drop(1).removePrefix(" "))
                    i++
                }
                val m = calloutRe.find(body[0])
                if (m != null) {
                    val marker = m.groupValues[2]
                    out.add(
                        Callout(
                            m.groupValues[1], marker.isNotEmpty(), marker == "-", m.groupValues[3],
                            parseBlocks(body.drop(1).joinToString("\n")),
                        ),
                    )
                } else out.add(Quote(parseBlocks(body.joinToString("\n"))))
            }
            isTableStart(lines, i) -> {
                val header = splitCells(line)
                i += 2
                val rows = ArrayList<List<String>>()
                while (i < lines.size && lines[i].trimStart().startsWith("|")) rows.add(splitCells(lines[i++]))
                out.add(Table(header, rows))
            }
            headingRe.matches(t) -> {
                val m = headingRe.find(t)!!
                out.add(Heading(m.groupValues[1].length, m.groupValues[2]))
                i++
            }
            t == "---" -> { out.add(Rule); i++ }
            listRe.matches(line) -> {
                val m = listRe.find(line)!!
                val num = m.groupValues[3]
                out.add(ListItem(num.isNotEmpty(), num.toIntOrNull() ?: 0, m.groupValues[1].length / 2, m.groupValues[4]))
                i++
            }
            imageRe.matches(t) -> { out.add(ImageBlock(imageRe.find(t)!!.groupValues[1])); i++ }
            else -> {
                val buf = arrayListOf(line)
                i++
                while (i < lines.size && !startsBlock(lines, i)) buf.add(lines[i++])
                out.add(Paragraph(buf.joinToString("\n")))
            }
        }
    }
    return out
}

/** Splits a table row on `|` outside backticks and `[[...]]`. */
private fun splitCells(row: String): List<String> {
    var s = row.trim()
    if (s.startsWith("|")) s = s.drop(1)
    if (s.endsWith("|")) s = s.dropLast(1)
    val cells = ArrayList<String>()
    val cur = StringBuilder()
    var tick = false
    var link = false
    var k = 0
    while (k < s.length) {
        val c = s[k]
        when {
            c == '`' -> { tick = !tick; cur.append(c) }
            !tick && s.startsWith("[[", k) -> { link = true; cur.append("[["); k++ }
            !tick && link && s.startsWith("]]", k) -> { link = false; cur.append("]]"); k++ }
            c == '|' && !tick && !link -> { cells.add(cur.toString().trim()); cur.clear() }
            else -> cur.append(c)
        }
        k++
    }
    cells.add(cur.toString().trim())
    return cells
}

fun parseInline(s: String): List<Inline> {
    val out = ArrayList<Inline>()
    val buf = StringBuilder()
    fun flush() { if (buf.isNotEmpty()) { out.add(Text(buf.toString())); buf.clear() } }
    var i = 0
    while (i < s.length) {
        val c = s[i]
        var end = -1
        when {
            c == '`' -> {
                end = s.indexOf('`', i + 1)
                if (end > i) { flush(); out.add(InlineCode(s.substring(i + 1, end))); i = end + 1; continue }
            }
            s.startsWith("==", i) && i + 2 < s.length && !s[i + 2].isWhitespace() && s[i + 2] != '=' &&
                (i == 0 || s[i - 1] != '=') -> {
                // Close at the next `==` that follows a non-space: «a == b» never opens, «==x» without a close stays literal.
                end = s.indexOf("==", i + 3)
                while (end > 0 && s[end - 1].isWhitespace()) end = s.indexOf("==", end + 1)
                if (end > i + 2) { flush(); out.add(Mark(s.substring(i + 2, end))); i = end + 2; continue }
            }
            s.startsWith("**", i) -> {
                end = s.indexOf("**", i + 2)
                if (end > i + 2) { flush(); out.add(Bold(s.substring(i + 2, end))); i = end + 2; continue }
            }
            c == '*' && i + 1 < s.length && !s[i + 1].isWhitespace() -> {
                end = s.indexOf('*', i + 1)
                if (end > i + 1) { flush(); out.add(Italic(s.substring(i + 1, end))); i = end + 1; continue }
            }
            s.startsWith("[[", i) -> {
                end = s.indexOf("]]", i + 2)
                if (end > i + 2) {
                    flush()
                    val body = s.substring(i + 2, end)
                    val alias = body.substringAfter('|', "").ifEmpty { null }
                    val ref = body.substringBefore('|')
                    val target = ref.substringBefore('#')
                    out.add(Link(target, ref.substringAfter('#', "").ifEmpty { null }, alias ?: target))
                    i = end + 2
                    continue
                }
            }
            c == '[' -> {
                // Standard `[label](url)`: `#anchor` -> same-note link (target "#anchor"), external url -> plain label (offline).
                val close = s.indexOf(']', i + 1)
                end = if (close > i + 1 && s.startsWith("(", close + 1)) s.indexOf(')', close + 2) else -1
                if (end > close + 1) {
                    flush()
                    val label = s.substring(i + 1, close)
                    val url = s.substring(close + 2, end).trim()
                    out.add(if (url.startsWith("#") && url.length > 1) Link(url, url.drop(1), label) else Text(label))
                    i = end + 1
                    continue
                }
            }
        }
        buf.append(c)
        i++
    }
    flush()
    return out
}
