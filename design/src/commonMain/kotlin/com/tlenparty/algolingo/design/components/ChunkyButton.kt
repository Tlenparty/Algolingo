package com.tlenparty.algolingo.design.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.design.AlgoTheme

enum class ChunkyStyle { Primary, Secondary, Danger, Amber }

/** Shared 3D block: [ledge] shows AlgoTheme.sizes.ledge under the face; pressing slides the face down over it. */
@Composable
fun ChunkyBox(
    face: Color,
    ledge: Color?,
    border: Color?,
    shape: Shape,
    faceMinHeight: Dp,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed && enabled && ledge != null) AlgoTheme.sizes.ledge else 0.dp, tween(80))
    val click = if (onClick != null) {
        Modifier.clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
    } else Modifier
    // propagateMinConstraints: a fixed/full width on [modifier] reaches the face; wrap-content callers are unaffected.
    Box(modifier.then(click), propagateMinConstraints = true) {
        if (ledge != null) Box(Modifier.matchParentSize().background(ledge, shape))
        Box(
            Modifier.padding(bottom = AlgoTheme.sizes.ledge)
                .offset { IntOffset(0, drop.roundToPx()) }
                .heightIn(min = faceMinHeight)
                .background(face, shape)
                .let { if (border != null) it.border(AlgoTheme.sizes.border, border, shape) else it },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

private data class Look(val face: Color, val ledge: Color?, val border: Color?, val label: Color)

/** Fills the available width; constrain it with e.g. `Modifier.width(...)` in [modifier]. */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ChunkyStyle = ChunkyStyle.Primary,
) {
    val c = AlgoTheme.colors
    val s = MaterialTheme.colorScheme
    val ink = s.onPrimary
    val look = when {
        !enabled -> Look(c.disabledFace, null, null, c.disabledLabel)
        style == ChunkyStyle.Primary -> Look(s.primary, c.greenLedge, null, ink)
        style == ChunkyStyle.Danger -> Look(c.redFill, c.redLedge, null, ink)
        style == ChunkyStyle.Amber -> Look(c.amber, c.amberLedge, null, ink)
        else -> Look(s.background, s.outline, s.outline, s.onBackground)
    }
    ChunkyBox(
        face = look.face, ledge = look.ledge, border = look.border, shape = AlgoTheme.shapes.large,
        faceMinHeight = AlgoTheme.sizes.buttonFace, enabled = enabled, onClick = onClick, modifier = modifier.fillMaxWidth(),
    ) {
        Text(text, color = look.label, style = AlgoTheme.typography.button, textAlign = TextAlign.Center)
    }
}
