package com.tlenparty.algolingo.lesson.data

import com.tlenparty.algolingo.lesson.domain.Accepted
import com.tlenparty.algolingo.lesson.domain.Chip
import com.tlenparty.algolingo.lesson.domain.Exercise
import com.tlenparty.algolingo.lesson.domain.Hole
import com.tlenparty.algolingo.lesson.domain.Lesson
import com.tlenparty.algolingo.lesson.domain.LessonKind
import com.tlenparty.algolingo.lesson.domain.Solution
import com.tlenparty.algolingo.lesson.domain.TheoryCard
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The lesson JSON format. Field names and defaults here are the contract with tools/gen_content.py.

@Serializable
enum class LessonKindDto {
    @SerialName("code") Code,
    @SerialName("design") Design,
}

@Serializable
data class LessonDto(
    val schema: Int = 1,
    val id: String,
    val kind: LessonKindDto = LessonKindDto.Code,
    val level: Int = 0,
    val title: String,
    val statement: String,
    val trains: String? = null,
    val targetTimeSec: Int? = null,
    val theory: List<TheoryCardDto> = emptyList(),
    val exercises: List<ExerciseDto>,
    val solution: SolutionDto,
)

@Serializable
data class TheoryCardDto(
    val title: String,
    val body: String = "",
    val signals: List<String> = emptyList(),
    val skeleton: String? = null,
    val pitfalls: List<String> = emptyList(),
)

@Serializable
data class ExerciseDto(
    val id: String,
    val segments: List<String>,
    val holes: List<HoleDto>,
    val bank: List<ChipDto>,
    val hints: List<String> = emptyList(),
    val title: String? = null,
)

@Serializable
data class HoleDto(
    val ok: List<String>,
    val acceptable: List<AcceptedDto> = emptyList(),
    val wrongWhy: Map<String, String> = emptyMap(),
)

@Serializable
data class AcceptedDto(val text: String, val note: String)

@Serializable
data class ChipDto(val text: String, val why: String? = null)

@Serializable
data class SolutionDto(
    val code: String,
    val time: String = "",
    val space: String = "",
    val pitfalls: List<String> = emptyList(),
    val variations: List<String> = emptyList(),
)

fun LessonDto.toDomain() = Lesson(
    schema, id, kind.toDomain(), level, title, statement, trains, targetTimeSec,
    theory.map { it.toDomain() }, exercises.map { it.toDomain() }, solution.toDomain(),
)

private fun LessonKindDto.toDomain() = when (this) {
    LessonKindDto.Code -> LessonKind.Code
    LessonKindDto.Design -> LessonKind.Design
}

private fun TheoryCardDto.toDomain() = TheoryCard(title, body, signals, skeleton, pitfalls)

fun ExerciseDto.toDomain() = Exercise(id, segments, holes.map { it.toDomain() }, bank.map { Chip(it.text, it.why) }, hints, title)

private fun HoleDto.toDomain() = Hole(ok, acceptable.map { Accepted(it.text, it.note) }, wrongWhy)

private fun SolutionDto.toDomain() = Solution(code, time, space, pitfalls, variations)
