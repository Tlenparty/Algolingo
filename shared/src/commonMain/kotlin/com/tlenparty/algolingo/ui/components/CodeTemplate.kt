package com.tlenparty.algolingo.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.lesson.HoleVisual
import com.tlenparty.algolingo.lesson.Piece
import com.tlenparty.algolingo.lesson.templateLines
import com.tlenparty.algolingo.ui.theme.LocalAppColors
import com.tlenparty.algolingo.ui.theme.LocalAppText
import com.tlenparty.algolingo.ui.theme.highlight

@Composable
fun CodeTemplate(
    segments: List<String>,
    placedText: List<String?>,
    visuals: List<HoleVisual>,
    onHoleTap: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val t = LocalAppText.current
    val lines = templateLines(segments)
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(16.dp)) {
        // One scroll for all lines; no fillMaxWidth inside (infinite width constraint).
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            for (line in lines) {
                Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (p in line) when (p) {
                        is Piece.Code -> Text(
                            highlight(p.s, colors), style = t.codeRegular,
                            color = MaterialTheme.colorScheme.onBackground, softWrap = false, maxLines = 1,
                        )
                        is Piece.HoleAt -> Hole(
                            p.h, placedText.getOrNull(p.h), visuals.getOrNull(p.h) ?: HoleVisual.Empty,
                            { onHoleTap(p.h) }, enabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hole(index: Int, text: String?, visual: HoleVisual, onTap: () -> Unit, enabled: Boolean) {
    val c = LocalAppColors.current
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
    val desc = when {
        text != null -> "Дырка ${index + 1}: $text"
        visual == HoleVisual.EmptySelected -> "Дырка ${index + 1}: выбрана"
        else -> "Дырка ${index + 1}: пусто"
    }
    // Touch area 40dp x >=48dp; drawn face 32dp.
    Box(
        Modifier.height(40.dp).widthIn(min = 48.dp)
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .clickable(enabled = enabled, role = Role.Button, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(8.dp)
        Box(
            Modifier.heightIn(min = 32.dp).widthIn(min = 48.dp).background(fill, shape).border(2.dp, border, shape)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(text, animationSpec = tween(100)) { shown ->
                if (shown != null) {
                    Text(
                        highlight(shown, c), style = LocalAppText.current.codeBold, color = ink,
                        softWrap = false, maxLines = 1,
                    )
                }
            }
        }
    }
}
