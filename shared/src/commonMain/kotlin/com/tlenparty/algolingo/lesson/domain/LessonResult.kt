package com.tlenparty.algolingo.lesson.domain

enum class Grade { Clean, Hinted, Failed }

data class ExerciseResult(val id: String, val solved: Boolean, val attempts: Int, val hintsShown: Int)

data class LessonResult(val grade: Grade, val perExercise: List<ExerciseResult>)
