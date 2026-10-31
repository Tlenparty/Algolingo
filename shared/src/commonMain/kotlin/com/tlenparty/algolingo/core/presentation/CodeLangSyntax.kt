package com.tlenparty.algolingo.core.presentation

import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.design.SyntaxLang

val CodeLang.syntax: SyntaxLang
    get() = when (this) {
        CodeLang.Kotlin -> SyntaxLang.Kotlin
        CodeLang.Python -> SyntaxLang.Python
    }
