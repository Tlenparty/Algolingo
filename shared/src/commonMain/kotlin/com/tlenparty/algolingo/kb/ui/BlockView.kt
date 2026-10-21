package com.tlenparty.algolingo.kb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.kb.Block
import com.tlenparty.algolingo.kb.Bold
import com.tlenparty.algolingo.kb.Callout
import com.tlenparty.algolingo.kb.Code
import com.tlenparty.algolingo.kb.Heading
import com.tlenparty.algolingo.kb.Inline
import com.tlenparty.algolingo.kb.InlineCode
import com.tlenparty.algolingo.kb.Italic
import com.tlenparty.algolingo.kb.Link
import com.tlenparty.algolingo.kb.ListItem
import com.tlenparty.algolingo.kb.Paragraph
import com.tlenparty.algolingo.kb.Quote
import com.tlenparty.algolingo.kb.Rule
import com.tlenparty.algolingo.kb.Table
import com.tlenparty.algolingo.kb.parseInline
import com.tlenparty.algolingo.ui.theme.highlight
import com.tlenparty.algolingo.ui.theme.LocalAppColors
import com.tlenparty.algolingo.kb.Text as PlainText

@Composable
fun inlineText(
    inlines: List<Inline>,
    resolvable: (String) -> Boolean = { false },
    onLink: (String, String?) -> Unit = { _, _ -> },
): AnnotatedString {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val link = LocalAppColors.current.focusText
    return buildAnnotatedString {
        for (n in inlines) when (n) {
            is PlainText -> append(n.v)
            is Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(n.v) }
            is Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(n.v) }
            is InlineCode -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = bg)) { append(n.v) }
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
}

@Composable
private fun Inl(
    text: String, modifier: Modifier = Modifier, bold: Boolean = false,
    resolvable: (String) -> Boolean, onLink: (String, String?) -> Unit,
) =
    Text(
        inlineText(parseInline(text), resolvable, onLink), modifier,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.Bold else null,
    )

@Composable
fun BlockView(
    block: Block,
    noteId: String = "",
    index: Int = 0,
    resolvable: (String) -> Boolean = { false },
    onLink: (String, String?) -> Unit = { _, _ -> },
) {
    val pad = Modifier.padding(vertical = 4.dp)
    when (block) {
        is Heading -> Text(
            inlineText(parseInline(block.text), resolvable, onLink),
            pad.padding(top = 8.dp),
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
        )
        is Paragraph -> Inl(block.text, pad, resolvable = resolvable, onLink = onLink)
        is ListItem -> Row(pad.padding(start = (block.depth * 16).dp)) {
            Text(if (block.ordered) "${block.number}." else "•", Modifier.width(24.dp))
            Inl(block.text, resolvable = resolvable, onLink = onLink)
        }
        is Code -> {
            val colors = LocalAppColors.current
            Surface(pad.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    if (block.lang == "kotlin") highlight(block.text, colors) else AnnotatedString(block.text),
                    Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    softWrap = false,
                )
            }
        }
        is Table -> Column(pad.horizontalScroll(rememberScrollState())) {
            val all = listOf(block.header) + block.rows
            // One width per column so cells line up across rows. ponytail: width from char count, measure text if it looks off.
            val widths = (0 until all.maxOf { it.size }).map { c ->
                (all.maxOf { it.getOrNull(c)?.length ?: 0 } * 8 + 16).coerceIn(80, 240).dp
            }
            for ((r, row) in all.withIndex()) {
                Row {
                    for ((c, cell) in row.withIndex()) Inl(cell, Modifier.width(widths[c]).padding(4.dp), bold = r == 0, resolvable = resolvable, onLink = onLink)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
        Rule -> HorizontalDivider(pad, color = MaterialTheme.colorScheme.outline)
        is Quote -> QuoteBox(block.blocks, noteId, resolvable, onLink)
        is Callout -> CalloutView(block, noteId, index) { BlockView(it, noteId, index, resolvable, onLink) }
    }
}

@Composable
private fun QuoteBox(
    blocks: List<Block>, noteId: String,
    resolvable: (String) -> Boolean, onLink: (String, String?) -> Unit,
) {
    Row(Modifier.padding(vertical = 4.dp).height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
        Column(Modifier.padding(start = 8.dp)) { blocks.forEach { BlockView(it, noteId, 0, resolvable, onLink) } }
    }
}
