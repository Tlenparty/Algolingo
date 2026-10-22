package com.tlenparty.algolingo.kb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownParserTest {
    @Test
    fun headingAndParagraph() =
        assertEquals(listOf(Heading(1, "T"), Paragraph("text")), parseBlocks("# T\n\ntext"))

    @Test
    fun softBreaksKept() = assertEquals(listOf(Paragraph("a\nb")), parseBlocks("a\nb"))

    @Test
    fun lists() {
        assertEquals(
            listOf(ListItem(false, 0, 0, "a"), ListItem(false, 0, 1, "b"), ListItem(true, 1, 0, "c")),
            parseBlocks("- a\n  - b\n1. c"),
        )
        assertEquals(
            listOf(ListItem(true, 1, 0, "a"), ListItem(true, 2, 0, "b")),
            parseBlocks("1. a\n2. b"),
        )
    }

    @Test
    fun fences() {
        assertEquals(listOf(Code("kotlin", "val x = 1")), parseBlocks("```kotlin\nval x = 1\n```"))
        assertEquals(listOf(Code("", "a")), parseBlocks("```\na\n```"))
        assertEquals(listOf(Code("kotlin", "a\nb")), parseBlocks("```kotlin\na\nb"))
        assertEquals(listOf(Code("", "2 > 2? нет")), parseBlocks("```\n2 > 2? нет\n```"))
    }

    @Test
    fun calloutWithCode() {
        val b = parseBlocks("> [!question]- Подсказка 1\n> текст\n> ```kotlin\n> val a = 1\n> ```")
        assertEquals(
            listOf(Callout("question", true, true, "Подсказка 1", listOf(Paragraph("текст"), Code("kotlin", "val a = 1")))),
            b,
        )
    }

    @Test
    fun calloutFlags() {
        val tip = parseBlocks("> [!tip] T").single() as Callout
        assertEquals(false, tip.foldable)
        val note = parseBlocks("> [!note]+ N").single() as Callout
        assertTrue(note.foldable)
        assertEquals(false, note.collapsed)
    }

    @Test
    fun twoCalloutsSplitByBlank() {
        val b = parseBlocks("> [!tip] A\n> x\n\n> [!tip] B\n> y")
        assertEquals(2, b.count { it is Callout })
    }

    @Test
    fun quote() =
        assertEquals(
            listOf(Quote(listOf(Paragraph("plain"), ListItem(false, 0, 0, "item")))),
            parseBlocks("> plain\n> - item"),
        )

    @Test
    fun table() =
        assertEquals(
            listOf(Table(listOf("a", "[[X|y]]"), listOf(listOf("1", "2")))),
            parseBlocks("| a | [[X|y]] |\n|---|:-:|\n| 1 | 2 |"),
        )

    @Test
    fun rule() = assertEquals(listOf(Rule), parseBlocks("---"))

    @Test
    fun inline() {
        assertEquals(
            listOf(Bold("b"), Text(" "), Italic("i"), Text(" "), InlineCode("c"), Text(" t")),
            parseInline("**b** *i* `c` t"),
        )
        assertEquals(listOf(Link("A", "h", "alias")), parseInline("[[A#h|alias]]"))
        assertEquals(listOf(Link("A", null, "A")), parseInline("[[A]]"))
        assertEquals(listOf(InlineCode("[[x]]")), parseInline("`[[x]]`"))
    }

    @Test
    fun markdownLinks() {
        assertEquals(
            listOf(Link("#1-hash-map--частоты", "1-hash-map--частоты", "Hash"), Text(" · "), Text("site")),
            parseInline("[Hash](#1-hash-map--частоты) · [site](https://x.y)"),
        )
        assertEquals("[a] (b) [c]", parseInline("[a] (b) [c]").joinToString("") { (it as Text).v })
    }

    @Test
    fun unclosedIsLiteral() {
        assertEquals("a **b", parseInline("a **b").joinToString("") { (it as Text).v })
        assertEquals("[[x", parseInline("[[x").joinToString("") { (it as Text).v })
    }

    @Test
    fun imageBlocks() {
        assertEquals(listOf(ImageBlock("ia1b2c3d4.png")), parseBlocks("![[ia1b2c3d4.png]]"))
        assertEquals(listOf(ImageBlock("x.png")), parseBlocks(" ![[x.png]]"))
        assertEquals(listOf(ImageBlock("x.png")), parseBlocks("![[x.png|300]]"))
        assertEquals(
            listOf(Paragraph("text"), ImageBlock("x.png"), Paragraph("more")),
            parseBlocks("text\n![[x.png]]\nmore"),
        )
        assertEquals(listOf(Paragraph("see ![[x.png]] inline")), parseBlocks("see ![[x.png]] inline"))
    }

    @Test
    fun mark() {
        assertEquals(listOf(Mark("важно")), parseInline("==важно=="))
        assertEquals(listOf(Text("a "), Mark("**b**"), Text(" c")), parseInline("a ==**b**== c"))
        for (s in listOf("сумма == target", "a === b", "==без конца")) assertEquals(listOf(Text(s)), parseInline(s))
        assertEquals(listOf(InlineCode("a==b")), parseInline("`a==b`"))
        // Space before the closing `==` still closes (real notes), and does not swallow a later `==`.
        assertEquals(listOf(Mark("a"), Text("b "), Mark("c"), Text(" d")), parseInline("==a ==b ==c== d"))
        assertEquals(listOf(Text("x "), Mark("важно"), Text(" y")), parseInline("x ==важно == y"))
    }
}
