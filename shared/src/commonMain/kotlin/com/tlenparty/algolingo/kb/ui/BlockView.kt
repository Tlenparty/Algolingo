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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
fun inlineText(inlines: List<Inline>): AnnotatedString {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val link = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        for (n in inlines) when (n) {
            is PlainText -> append(n.v)
            is Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(n.v) }
            is Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(n.v) }
            is InlineCode -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = bg)) { append(n.v) }
            is Link -> withStyle(SpanStyle(color = link, textDecoration = TextDecoration.Underline)) { append(n.label) }
        }
    }
}

@Composable
private fun Inl(text: String, modifier: Modifier = Modifier, bold: Boolean = false) =
    Text(
        inlineText(parseInline(text)), modifier,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.Bold else null,
    )

@Composable
fun BlockView(block: Block, onLink: (String, String?) -> Unit = { _, _ -> }) {
    val pad = Modifier.padding(vertical = 4.dp)
    when (block) {
        is Heading -> Text(
            inlineText(parseInline(block.text)),
            pad.padding(top = 8.dp),
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
        )
        is Paragraph -> Inl(block.text, pad)
        is ListItem -> Row(pad.padding(start = (block.depth * 16).dp)) {
            Text(if (block.ordered) "${block.number}." else "•", Modifier.width(24.dp))
            Inl(block.text)
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
            for ((r, row) in (listOf(block.header) + block.rows).withIndex()) {
                Row {
                    for (cell in row) Inl(cell, Modifier.widthIn(min = 120.dp, max = 240.dp).padding(4.dp), bold = r == 0)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
        Rule -> HorizontalDivider(pad, color = MaterialTheme.colorScheme.outline)
        is Quote -> QuoteBox(block.blocks, onLink)
        // plan 03 replaces this with a real callout
        is Callout -> QuoteBox(listOf<Block>(Paragraph("**${block.title}**")) + block.blocks, onLink)
    }
}

@Composable
private fun QuoteBox(blocks: List<Block>, onLink: (String, String?) -> Unit) {
    Row(Modifier.padding(vertical = 4.dp).height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
        Column(Modifier.padding(start = 8.dp)) { blocks.forEach { BlockView(it, onLink) } }
    }
}
