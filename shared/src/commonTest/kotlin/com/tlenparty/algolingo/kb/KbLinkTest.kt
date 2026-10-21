package com.tlenparty.algolingo.kb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KbLinkTest {
    private val notes = listOf(
        Note("n1", "BIG O", "Темы", ""),
        Note("n2", "X", "Темы", ""),
    )

    @Test fun caseInsensitiveHit() = assertEquals("n1", resolveNote(notes, " Big O ")?.id)
    @Test fun missingIsNull() = assertNull(resolveNote(notes, "Missing"))
    @Test fun aliasAndHeadingStripped() {
        assertEquals("n2", resolveNote(notes, "X#Heading|alias")?.id)
        assertEquals("n2", resolveNote(notes, "X|alias")?.id)
    }

    private val blocks = parseBlocks("# T\n\ntext\n\n## Heading\n\nmore")

    @Test fun anchorHit() = assertEquals(2, findAnchorIndex(blocks, "heading"))
    @Test fun anchorBySlug() = assertEquals(
        0,
        findAnchorIndex(parseBlocks("## 10. Монотонный стек — ближайший больший/меньший"), "10-монотонный-стек--ближайший-большийменьший"),
    )
    @Test fun anchorMiss() = assertNull(findAnchorIndex(blocks, "nope"))
}
