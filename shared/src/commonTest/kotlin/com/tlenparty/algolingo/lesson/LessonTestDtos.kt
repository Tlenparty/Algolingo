package com.tlenparty.algolingo.lesson

import com.tlenparty.algolingo.lesson.data.AcceptedDto
import com.tlenparty.algolingo.lesson.data.ChipDto
import com.tlenparty.algolingo.lesson.data.ExerciseDto
import com.tlenparty.algolingo.lesson.data.HoleDto
import com.tlenparty.algolingo.lesson.data.LessonDto
import com.tlenparty.algolingo.lesson.data.LessonKindDto
import com.tlenparty.algolingo.lesson.data.SolutionDto
import com.tlenparty.algolingo.lesson.data.TheoryCardDto
import com.tlenparty.algolingo.lesson.domain.Lesson
import com.tlenparty.algolingo.lesson.domain.LessonKind

/** Tests write lessons as JSON through this; the app itself only reads them. */
fun Lesson.toDto() = LessonDto(
    schema, id,
    if (kind == LessonKind.Design) LessonKindDto.Design else LessonKindDto.Code,
    level, title, statement, trains, targetTimeSec,
    theory.map { TheoryCardDto(it.title, it.body, it.signals, it.skeleton, it.pitfalls) },
    exercises.map { e ->
        ExerciseDto(
            e.id, e.segments,
            e.holes.map { h -> HoleDto(h.ok, h.acceptable.map { AcceptedDto(it.text, it.note) }, h.wrongWhy) },
            e.bank.map { ChipDto(it.text, it.why) }, e.hints, e.title,
        )
    },
    SolutionDto(solution.code, solution.time, solution.space, solution.pitfalls, solution.variations),
)
