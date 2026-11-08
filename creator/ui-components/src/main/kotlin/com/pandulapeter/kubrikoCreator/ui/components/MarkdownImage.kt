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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import org.jetbrains.skia.Image as SkiaImage

/**
 * An image of a Markdown text, read from [file] off the main thread and drawn no wider than it is (in the interface's
 * scale) or than there is room for; [fallback] stands in while there is no file, while it is read, and when it can't be.
 */
@Composable
internal fun MarkdownImage(
    block: MarkdownBlock.Image,
    file: File?,
    fallback: @Composable () -> Unit,
) {
    val bitmap = produceState<ImageBitmap?>(null, file, file?.lastModified()) {
        value = file?.let { withContext(Dispatchers.IO) { runCatching { SkiaImage.makeFromEncoded(it.readBytes()).toComposeImageBitmap() }.getOrNull() } }
    }.value
    if (bitmap == null) {
        fallback()
        return
    }
    val width = with(LocalDensity.current) { bitmap.width.toDp() }
    Image(
        modifier = Modifier.widthIn(max = width).clip(RoundedCornerShape(6.dp)),
        bitmap = bitmap,
        contentDescription = block.alt.ifBlank { null },
        contentScale = ContentScale.FillWidth,
    )
}
