package com.tlenparty.algolingo.lesson.domain

enum class LessonKind {
    Code,
    Design,
}

data class Lesson(
    val schema: Int = 1,
    val id: String,
    val kind: LessonKind = LessonKind.Code,
    val level: Int = 0,
    val title: String,
    val statement: String,
    val trains: String? = null,
    val targetTimeSec: Int? = null,
    val theory: List<TheoryCard> = emptyList(),
    val exercises: List<Exercise>,
    val solution: Solution,
)

data class TheoryCard(
    val title: String,
    val body: String = "",
    val signals: List<String> = emptyList(),
    val skeleton: String? = null,
    val pitfalls: List<String> = emptyList(),
)

data class Exercise(
    val id: String,
    val segments: List<String>,
    val holes: List<Hole>,
    val bank: List<Chip>,
    val hints: List<String> = emptyList(),
    /** Design step caption («Шаг 2 · API»), shown above the template. */
    val title: String? = null,
)

data class Hole(
    val ok: List<String>,
    val acceptable: List<Accepted> = emptyList(),
    val wrongWhy: Map<String, String> = emptyMap(),
)

data class Accepted(val text: String, val note: String)

// Chip identity = index in Exercise.bank (EXER-05); no id field on purpose.
data class Chip(val text: String, val why: String? = null)

data class Solution(
    val code: String,
    val time: String = "",
    val space: String = "",
    val pitfalls: List<String> = emptyList(),
    val variations: List<String> = emptyList(),
)
