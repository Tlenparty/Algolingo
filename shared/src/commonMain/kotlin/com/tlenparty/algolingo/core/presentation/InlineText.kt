package com.tlenparty.algolingo.core.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.tlenparty.algolingo.core.markdown.Bold
import com.tlenparty.algolingo.core.markdown.Inline
import com.tlenparty.algolingo.core.markdown.InlineCode
import com.tlenparty.algolingo.core.markdown.Italic
import com.tlenparty.algolingo.core.markdown.Link
import com.tlenparty.algolingo.core.markdown.Mark
import com.tlenparty.algolingo.core.markdown.parseInline
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.design.NO_LIGATURES
import com.tlenparty.algolingo.core.markdown.Text as PlainText

@Composable
fun inlineText(
    inlines: List<Inline>,
    resolvable: (String) -> Boolean = { false },
    onLink: (String, String?) -> Unit = { _, _ -> },
): AnnotatedString {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val link = AlgoTheme.colors.focusText
    val mono = AlgoTheme.typography.code
    val markBg = AlgoTheme.colors.acceptableTint
    fun AnnotatedString.Builder.add(inlines: List<Inline>) {
        for (n in inlines) when (n) {
            is PlainText -> append(n.v)
            is Mark -> withStyle(SpanStyle(background = markBg)) { add(parseInline(n.v)) }
            is Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(n.v) }
            is Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(n.v) }
            is InlineCode -> withStyle(SpanStyle(fontFamily = mono, fontFeatureSettings = NO_LIGATURES, background = bg)) { append(n.v) }
            is Link ->
                if (resolvable(n.target)) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "kb:${n.target}",
                            styles = TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline)),
                        ) { onLink(n.target, n.anchor) },
                    ) { append(n.label) }
                } else append(n.label)
        }
    }
    return buildAnnotatedString { add(inlines) }
}
