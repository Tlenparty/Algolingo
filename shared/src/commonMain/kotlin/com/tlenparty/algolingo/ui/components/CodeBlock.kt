package com.tlenparty.algolingo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tlenparty.algolingo.ui.theme.LocalAppColors
import com.tlenparty.algolingo.ui.theme.LocalAppText
import com.tlenparty.algolingo.ui.theme.highlight

@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier, highlightRanges: List<IntRange> = emptyList()) {
    val colors = LocalAppColors.current
    val text = buildAnnotatedString {
        append(highlight(code, colors))
        for (r in highlightRanges) {
            val start = r.first.coerceIn(0, code.length)
            val end = (r.last + 1).coerceIn(start, code.length)
            if (end > start) addStyle(SpanStyle(background = colors.successTint), start, end)
        }
    }
    Text(
        text,
        style = LocalAppText.current.codeRegular.copy(lineHeight = 20.sp),
        color = MaterialTheme.colorScheme.onBackground,
        softWrap = false,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .horizontalScroll(rememberScrollState()).padding(16.dp),
    )
}
