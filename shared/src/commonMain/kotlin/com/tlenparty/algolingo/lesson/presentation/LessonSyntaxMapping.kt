package com.tlenparty.algolingo.lesson.presentation

import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.core.presentation.syntax
import com.tlenparty.algolingo.design.SyntaxLang
import com.tlenparty.algolingo.lesson.domain.Lesson
import com.tlenparty.algolingo.lesson.domain.LessonSyntax

/**
 * Syntax rules for a lesson's code components: the lesson's own [Lesson.syntax] when set (Backend lessons),
 * else the solution language from the route. Plain has no rules; callers check [Lesson.highlights] first.
 */
fun Lesson.syntaxLang(lang: CodeLang): SyntaxLang = when (syntax) {
    null, LessonSyntax.Plain -> lang.syntax
    LessonSyntax.Kotlin -> SyntaxLang.Kotlin
    LessonSyntax.Python -> SyntaxLang.Python
    LessonSyntax.Sql -> SyntaxLang.Sql
    LessonSyntax.Yaml -> SyntaxLang.Yaml
    LessonSyntax.Http -> SyntaxLang.Http
}
