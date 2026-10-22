package com.tlenparty.algolingo.design.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.design.highlight

enum class ChipState { Normal, Ghost }

/** Bank chip. Ghost keeps exact size (text composed but transparent) so the bank does not reflow. */
@Composable
fun TokenChip(text: String, state: ChipState, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    val ghost = state == ChipState.Ghost
    ChunkyBox(
        face = if (ghost) s.outline else s.background,
        ledge = if (ghost) null else s.outline,
        border = if (ghost) null else s.outline,
        shape = AlgoTheme.shapes.medium,
        faceMinHeight = AlgoTheme.sizes.chipFace,
        enabled = !ghost && onClick != null,
        onClick = if (ghost) null else onClick,
        modifier = if (ghost) modifier.clearAndSetSemantics {} else modifier,
    ) {
        Text(
            // Ghost: plain text, otherwise highlight spans (numbers, keywords) override the transparent color.
            if (ghost) AnnotatedString(text) else highlight(text, AlgoTheme.colors),
            style = AlgoTheme.typography.codeChip,
            color = if (ghost) Color.Transparent else s.onBackground,
            softWrap = false,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = AlgoTheme.spacing.md),
        )
    }
}
