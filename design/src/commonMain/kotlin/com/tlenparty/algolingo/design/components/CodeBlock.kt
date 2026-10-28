package com.tlenparty.algolingo.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.design.highlight

@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier, highlightRanges: List<IntRange> = emptyList(), highlight: Boolean = true) {
    val colors = AlgoTheme.colors
    val text = buildAnnotatedString {
        if (highlight) append(com.tlenparty.algolingo.design.highlight(code, colors)) else append(code)
        for (r in highlightRanges) {
            val start = r.first.coerceIn(0, code.length)
            val end = (r.last + 1).coerceIn(start, code.length)
            if (end > start) addStyle(SpanStyle(background = colors.successTint), start, end)
        }
    }
    Text(
        text,
        style = AlgoTheme.typography.codeRegular,
        color = MaterialTheme.colorScheme.onBackground,
        softWrap = false,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, AlgoTheme.shapes.large)
            .horizontalScroll(rememberScrollState()).padding(AlgoTheme.spacing.md),
    )
}
