/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.action_clear_text
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.ic_close

/**
 * A desktop-sized text field: one line high, with an optional [label] inside it on the left and an optional
 * [placeholder] while it is empty. [isError] draws its outline in the error color. Multiple lines wrap, unless
 * [isWrapping] is false, as for code, when they scroll sideways instead. [isSecret] shows the text as dots, as for a key.
 * However narrow the field, the label leaves the text at least [MINIMUM_TEXT_WIDTH], ending in an ellipsis instead.
 * [isClearable] gives it a button at its end, shown while it holds text, that empties it, as for a search; its room is
 * kept while it is hidden, so the text never moves when it appears.
 *
 * @param textWidth How wide the text is, for a field as wide as its label and that, which then needs no width of its
 *   own; null for a field whose text takes whatever its width leaves.
 */
@Composable
public fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    isSingleLine: Boolean = true,
    isWrapping: Boolean = true,
    isSecret: Boolean = false,
    isClearable: Boolean = false,
    textWidth: Dp? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val onClear = rememberOnClear(isClearable) { onValueChange("") }
    BasicTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        singleLine = isSingleLine,
        visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Decoration(
                isEmpty = value.isEmpty(),
                label = label,
                placeholder = placeholder,
                isError = isError,
                isSingleLine = isSingleLine,
                isWrapping = isWrapping,
                onClear = onClear,
                textWidth = textWidth,
                textStyle = textStyle,
                innerTextField = innerTextField,
            )
        },
    )
}

/**
 * The [CompactTextField] of a [TextFieldValue], for a caller that places the cursor or the selection itself.
 */
@Composable
public fun CompactTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    isSingleLine: Boolean = true,
    isWrapping: Boolean = true,
    isClearable: Boolean = false,
    textWidth: Dp? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val onClear = rememberOnClear(isClearable) { onValueChange(TextFieldValue()) }
    BasicTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        singleLine = isSingleLine,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Decoration(
                isEmpty = value.text.isEmpty(),
                label = label,
                placeholder = placeholder,
                isError = isError,
                isSingleLine = isSingleLine,
                isWrapping = isWrapping,
                onClear = onClear,
                textWidth = textWidth,
                textStyle = textStyle,
                innerTextField = innerTextField,
            )
        },
    )
}

/**
 * What a clearable [CompactTextField]'s clear button does, the same lambda across recompositions while [clear] reads
 * the latest callback, so typing doesn't recompose the button; null for a field that isn't clearable.
 */
@Composable
private fun rememberOnClear(isClearable: Boolean, clear: () -> Unit): (() -> Unit)? {
    val latestClear by rememberUpdatedState(clear)
    return remember(isClearable) { if (isClearable) ({ latestClear() }) else null }
}

@Composable
private fun Decoration(
    isEmpty: Boolean,
    label: String?,
    placeholder: String?,
    isError: Boolean,
    isSingleLine: Boolean,
    isWrapping: Boolean,
    onClear: (() -> Unit)?,
    textWidth: Dp?,
    textStyle: TextStyle,
    innerTextField: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Layout(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .border(1.dp, if (isError) colors.error else colors.outlineVariant, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        content = {
            if (label != null) {
                Text(
                    modifier = Modifier.layoutId(LABEL_ID),
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(Modifier.layoutId(TEXT_ID).then(if (isWrapping) Modifier else Modifier.horizontalScroll(rememberScrollState()))) {
                if (isEmpty && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = colors.onSurfaceVariant,
                        maxLines = if (isSingleLine) 1 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
            if (onClear != null) {
                ClearButton(modifier = Modifier.layoutId(CLEAR_ID), isShown = !isEmpty, onClick = onClear)
            }
        },
    ) { measurables, constraints ->
        val gap = LABEL_GAP.roundToPx()
        val clearPlaceable = measurables.firstOrNull { it.layoutId == CLEAR_ID }?.measure(Constraints())
        val clearSpace = clearPlaceable?.let { it.width + CLEAR_GAP.roundToPx() } ?: 0
        val fixedTextWidth = textWidth?.roundToPx()
        val reservedTextWidth = fixedTextWidth ?: MINIMUM_TEXT_WIDTH.roundToPx()
        val labelPlaceable = measurables.firstOrNull { it.layoutId == LABEL_ID }?.let { label ->
            val labelMaxWidth = if (constraints.hasBoundedWidth) (constraints.maxWidth - gap - reservedTextWidth - clearSpace).coerceAtLeast(0) else Constraints.Infinity
            label.measure(Constraints(maxWidth = labelMaxWidth, maxHeight = constraints.maxHeight))
        }
        val labelSpace = labelPlaceable?.let { it.width + gap } ?: 0
        val textPlaceable = measurables.first { it.layoutId == TEXT_ID }.measure(
            when {
                fixedTextWidth != null -> Constraints.fixedWidth(fixedTextWidth).copy(maxHeight = constraints.maxHeight)
                constraints.hasBoundedWidth -> Constraints.fixedWidth((constraints.maxWidth - labelSpace - clearSpace).coerceAtLeast(0)).copy(maxHeight = constraints.maxHeight)
                else -> Constraints(maxHeight = constraints.maxHeight)
            },
        )
        val width = constraints.constrainWidth(labelSpace + textPlaceable.width + clearSpace)
        val height = constraints.constrainHeight(maxOf(labelPlaceable?.height ?: 0, textPlaceable.height, clearPlaceable?.height ?: 0))
        layout(width, height) {
            fun top(placeableHeight: Int) = if (isSingleLine) (height - placeableHeight) / 2 else 0
            labelPlaceable?.placeRelative(0, top(labelPlaceable.height))
            textPlaceable.placeRelative(labelSpace, top(textPlaceable.height))
            clearPlaceable?.placeRelative(width - clearPlaceable.width, top(clearPlaceable.height))
        }
    }
}

/**
 * The [CompactTextField]'s button that empties it, fading in while [isShown] in room kept for it either way. It never
 * takes the focus, so the field keeps it; a hidden one is gone, so it neither offers its tooltip nor takes a click.
 */
@Composable
private fun ClearButton(
    isShown: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) = Box(modifier.size(CLEAR_SIZE)) {
    AnimatedVisibility(
        visible = isShown,
        enter = fadeIn(CreatorMotion.short()),
        exit = fadeOut(CreatorMotion.short()),
    ) {
        IconActionButton(
            icon = Res.drawable.ic_close,
            label = Res.string.action_clear_text,
            onClick = onClick,
            isSmall = true,
            isFocusable = false,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val LABEL_GAP = 8.dp

private val CLEAR_GAP = 4.dp

private val CLEAR_SIZE = 20.dp

private const val LABEL_ID = "label"

private const val TEXT_ID = "text"

private const val CLEAR_ID = "clear"

/**
 * The least room a [CompactTextField]'s label leaves its text, so a long label can't hide what is typed.
 */
private val MINIMUM_TEXT_WIDTH = 40.dp
