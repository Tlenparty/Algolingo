package com.tlenparty.algolingo.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.design.AlgoTheme

/** Tinted card with an optional AlgoTheme.sizes.border outline: theory card, hint card, kb callout. */
@Composable
fun AlgoCard(
    color: Color,
    border: Color?,
    shape: Shape,
    padding: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier,
        shape = shape,
        color = color,
        border = border?.let { BorderStroke(AlgoTheme.sizes.border, it) },
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}
