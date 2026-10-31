package com.tlenparty.algolingo.kb.presentation

import algolingo.shared.generated.resources.Res
import androidx.compose.foundation.Image
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import com.tlenparty.algolingo.design.highlight
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.design.NO_LIGATURES
import org.jetbrains.compose.resources.decodeToImageBitmap
import com.tlenparty.algolingo.core.markdown.Block
import com.tlenparty.algolingo.core.markdown.Bold
import com.tlenparty.algolingo.core.markdown.Callout
import com.tlenparty.algolingo.core.markdown.Code
import com.tlenparty.algolingo.core.markdown.Heading
import com.tlenparty.algolingo.core.markdown.ImageBlock
import com.tlenparty.algolingo.core.markdown.ListItem
import com.tlenparty.algolingo.core.markdown.Paragraph
import com.tlenparty.algolingo.core.markdown.Quote
import com.tlenparty.algolingo.core.markdown.Rule
import com.tlenparty.algolingo.core.markdown.Table
import com.tlenparty.algolingo.core.markdown.Text as PlainText
import com.tlenparty.algolingo.core.markdown.parseInline
import com.tlenparty.algolingo.core.presentation.inlineText

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
    val pad = Modifier.padding(vertical = AlgoTheme.spacing.xs)
    when (block) {
        is Heading -> Text(
            inlineText(parseInline(block.text), resolvable, onLink),
            pad.padding(top = AlgoTheme.spacing.sm),
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
        )
        is Paragraph -> Inl(block.text, pad, resolvable = resolvable, onLink = onLink)
        is ListItem -> Row(pad.padding(start = AlgoTheme.spacing.md * block.depth)) {
            Text(if (block.ordered) "${block.number}." else "•", Modifier.width(AlgoTheme.sizes.listMarker))
            Inl(block.text, resolvable = resolvable, onLink = onLink)
        }
        is Code -> {
            val colors = AlgoTheme.colors
            val mono = AlgoTheme.typography.code
            Surface(pad.fillMaxWidth(), shape = AlgoTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    if (block.lang == "kotlin") highlight(block.text, colors) else AnnotatedString(block.text),
                    Modifier.horizontalScroll(rememberScrollState()).padding(AlgoTheme.spacing.smd),
                    fontFamily = mono,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = NO_LIGATURES),
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
                    for ((c, cell) in row.withIndex()) Inl(cell, Modifier.width(widths[c]).padding(AlgoTheme.spacing.xs), bold = r == 0, resolvable = resolvable, onLink = onLink)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
        Rule -> HorizontalDivider(pad, color = MaterialTheme.colorScheme.outline)
        is Quote -> QuoteBox(block.blocks, noteId, resolvable, onLink)
        is Callout -> CalloutView(block, noteId, index) { BlockView(it, noteId, index, resolvable, onLink) }
        is ImageBlock -> {
            // ponytail: the item grows after the async load, so an anchor scroll into an image note may land slightly off;
            // store the aspect ratio in the index if it bites.
            val bmp by produceState(imageCache[block.file], block.file) {
                if (value != null) return@produceState
                value = try {
                    Res.readBytes("files/kb/img/${block.file}").decodeToImageBitmap().also { imageCache[block.file] = it }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    null
                }
            }
            bmp?.let {
                Image(it, contentDescription = "Картинка из заметки", modifier = pad.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            } ?: Text("🖼 картинка", pad, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Survives LazyColumn item disposal so scrolling back does not re-decode and re-grow the row.
// ponytail: unbounded, fine for the bundled few dozen images; switch to an LRU if the bundle grows large.
private val imageCache = mutableMapOf<String, ImageBitmap>()

@Composable
private fun QuoteBox(
    blocks: List<Block>, noteId: String,
    resolvable: (String) -> Boolean, onLink: (String, String?) -> Unit,
) {
    Row(Modifier.padding(vertical = AlgoTheme.spacing.xs).height(IntrinsicSize.Min)) {
        Box(Modifier.width(AlgoTheme.sizes.quoteBar).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
        Column(Modifier.padding(start = AlgoTheme.spacing.sm)) { blocks.forEach { BlockView(it, noteId, 0, resolvable, onLink) } }
    }
}
