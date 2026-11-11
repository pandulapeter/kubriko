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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import org.jetbrains.skia.Image as SkiaImage

/**
 * The image in [file], read off the main thread, and read again once the file changes; null while there is no file,
 * while it is read, and when it can't be.
 */
@Composable
public fun rememberImageFile(file: File?): ImageBitmap? = produceState<ImageBitmap?>(null, file, file?.lastModified()) {
    value = file?.let { withContext(Dispatchers.IO) { runCatching { SkiaImage.makeFromEncoded(it.readBytes()).toComposeImageBitmap() }.getOrNull() } }
}.value
