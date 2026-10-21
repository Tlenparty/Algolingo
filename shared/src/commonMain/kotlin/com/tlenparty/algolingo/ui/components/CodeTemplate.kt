package com.tlenparty.algolingo.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tlenparty.algolingo.lesson.HoleVisual
import com.tlenparty.algolingo.lesson.Piece
import com.tlenparty.algolingo.lesson.templateLines
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.design.highlight

@Composable
fun CodeTemplate(
    segments: List<String>,
    placedText: List<String?>,
    visuals: List<HoleVisual>,
    selected: Int?,
    onHoleTap: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AlgoTheme.colors
    val t = AlgoTheme.typography
    val lines = templateLines(segments)
    // Hole to keep on screen: the first Wrong after Check, else the selected one. With neither
    // (last hole filled, or all ok) scroll back to column 0 so the short `}` lines are visible again.
    val firstBad = visuals.indexOf(HoleVisual.Wrong)
    val focus = if (firstBad >= 0) firstBad else selected
    val hScroll = rememberScrollState()
    LaunchedEffect(focus) { if (focus == null) hScroll.animateScrollTo(0) }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant, AlgoTheme.shapes.large).padding(AlgoTheme.spacing.md)) {
        // One scroll for all lines; no fillMaxWidth inside (infinite width constraint).
        Column(Modifier.horizontalScroll(hScroll)) {
            for (line in lines) {
                // Only lines with a hole need the sizes.holeLine touch height; plain code lines stay compact.
                val rowHeight = if (line.any { it is Piece.HoleAt }) AlgoTheme.sizes.holeLine else AlgoTheme.sizes.codeLine
                Row(Modifier.height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                    for (p in line) when (p) {
                        is Piece.Code -> Text(
                            highlight(p.s, colors), style = t.codeRegular,
                            color = MaterialTheme.colorScheme.onBackground, softWrap = false, maxLines = 1,
                        )
                        is Piece.HoleAt -> Hole(
                            p.h, placedText.getOrNull(p.h), visuals.getOrNull(p.h) ?: HoleVisual.Empty,
                            { onHoleTap(p.h) }, enabled, focused = p.h == focus,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hole(index: Int, text: String?, visual: HoleVisual, onTap: () -> Unit, enabled: Boolean, focused: Boolean) {
    val c = AlgoTheme.colors
    val s = MaterialTheme.colorScheme
    // fill, border, text color
    val target = when (visual) {
        HoleVisual.Empty -> Triple(s.outline, Color.Transparent, s.onBackground)
        HoleVisual.EmptySelected -> Triple(c.focusTint, c.focus, s.onBackground)
        HoleVisual.Filled -> Triple(s.background, s.outline, s.onBackground)
        HoleVisual.Ok -> Triple(c.successTint, s.primary, c.successText)
        HoleVisual.Acceptable -> Triple(c.acceptableTint, c.amber, c.acceptableText)
        HoleVisual.Wrong -> Triple(c.wrongTint, c.redFill, c.wrongText)
    }
    val fill by animateColorAsState(target.first, tween(200))
    val border by animateColorAsState(target.second, tween(200))
    val ink by animateColorAsState(target.third, tween(200))
    val verdict = when (visual) {
        HoleVisual.Ok -> ": верно"
        HoleVisual.Acceptable -> ": принято"
        HoleVisual.Wrong -> ": ошибка"
        else -> ""
    }
    val desc = when {
        text != null -> "Дырка ${index + 1}: $text$verdict"
        visual == HoleVisual.EmptySelected -> "Дырка ${index + 1}: выбрана"
        else -> "Дырка ${index + 1}: пусто"
    }
    // Long lines scroll sideways: keep the hole the next chip lands in (or the first Wrong one) on screen.
    val inView = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) { if (focused) inView.bringIntoView() }
    // Touch area sizes.holeLine x >= sizes.touchTarget; drawn face sizes.holeFace.
    Box(
        Modifier.bringIntoViewRequester(inView).height(AlgoTheme.sizes.holeLine).widthIn(min = AlgoTheme.sizes.touchTarget)
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .clickable(enabled = enabled, role = Role.Button, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        val shape = AlgoTheme.shapes.small
        Box(
            Modifier.heightIn(min = AlgoTheme.sizes.holeFace).widthIn(min = AlgoTheme.sizes.holeMinWidth).background(fill, shape).border(AlgoTheme.sizes.border, border, shape)
                .padding(horizontal = AlgoTheme.spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(text, animationSpec = tween(100)) { shown ->
                if (shown != null) {
                    Text(
                        highlight(shown, c), style = AlgoTheme.typography.codeBold, color = ink,
                        softWrap = false, maxLines = 1,
                    )
                }
            }
        }
    }
}
