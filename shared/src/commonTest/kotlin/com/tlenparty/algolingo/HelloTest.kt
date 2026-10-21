package com.tlenparty.algolingo

import kotlin.test.Test
import kotlin.test.assertEquals

class HelloTest {

    @Test
    fun parsesHello() {
        assertEquals(
            Hello("Алголинго", "Скелет: JSON прочитан из ресурсов"),
            parseHello("""{"title":"Алголинго","subtitle":"Скелет: JSON прочитан из ресурсов"}"""),
        )
    }

    @Test
    fun ignoresUnknownKeys() {
        assertEquals(Hello("a", "b"), parseHello("""{"title":"a","subtitle":"b","extra":1}"""))
    }
}
