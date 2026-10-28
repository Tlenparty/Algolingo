package com.tlenparty.algolingo.design

import androidx.compose.runtime.compositionLocalOf

/** Solution language. [id] is the JSON form ("kotlin"/"python") used by content and progress. */
enum class CodeLang(val id: String) { Kotlin("kotlin"), Python("python") }

/** Set once by the lesson; code components read it. Default Kotlin keeps KB and SD unchanged. */
val LocalCodeLang = compositionLocalOf { CodeLang.Kotlin }
