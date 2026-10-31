package com.tlenparty.algolingo.design

import androidx.compose.runtime.compositionLocalOf

/** Syntax rules for code components. The design system knows nothing about course languages. */
enum class SyntaxLang { Kotlin, Python }

/** Set once by the lesson; code components read it. Default Kotlin keeps KB and SD unchanged. */
val LocalSyntaxLang = compositionLocalOf { SyntaxLang.Kotlin }
