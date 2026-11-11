/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.SpriteFrames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.io.File

/**
 * This image as one Compose draws, its pixels as they are (not premultiplied), so see-through ones stay see-through.
 */
internal fun PixelImage.toImageBitmap(): ImageBitmap {
    val pixels = toIntArray()
    val bytes = ByteArray(pixels.size * 4)
    pixels.forEachIndexed { index, argb ->
        bytes[index * 4] = (argb and 0xFF).toByte()
        bytes[index * 4 + 1] = (argb ushr 8 and 0xFF).toByte()
        bytes[index * 4 + 2] = (argb ushr 16 and 0xFF).toByte()
        bytes[index * 4 + 3] = (argb ushr 24 and 0xFF).toByte()
    }
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL))
    bitmap.installPixels(bytes)
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}

/**
 * The frames of the sprite in [file], cut as [sheet] says, read off the main thread whenever [revision] changes (an edit
 * written, or the files scanned again after a change outside), and decoded again only when the file's length or time
 * changed, since one write raises both; the frames read before stay shown meanwhile. Its caller shows one sprite (keyed by
 * it), so frames never outlive theirs.
 */
@Composable
internal fun rememberSpriteFrames(file: File?, sheet: SpriteSheet?, revision: Any?): SpriteFramesRead {
    val reader = remember { SpriteImageReader() }
    return produceState<SpriteFramesRead>(SpriteFramesRead.Reading, file, sheet, revision) {
        val image = file?.let { withContext(Dispatchers.IO) { reader.read(it) } }
        value = image?.let { SpriteFramesRead.Read(SpriteFrames.of(it, sheet?.takeIf { sheet -> sheet.problemWithImage(it.width, it.height) == null })) } ?: SpriteFramesRead.Unreadable
    }.value
}

/**
 * Reads sprites' images, decoding one again only when its file's length or time changed since it was last read. Blocks.
 */
internal class SpriteImageReader {
    private var lastFile: File? = null
    private var lastStamp: Pair<Long, Long>? = null
    private var lastImage: PixelImage? = null

    @Synchronized
    fun read(file: File): PixelImage? {
        val stamp = file.length() to file.lastModified()
        if (file != lastFile || stamp != lastStamp || lastImage == null) {
            lastImage = runCatching { PixelImage.of(file.readBytes()) }.getOrNull()
            lastFile = file
            lastStamp = stamp
        }
        return lastImage
    }
}

/**
 * What reading a sprite's frames ([rememberSpriteFrames]) came to so far.
 */
internal sealed interface SpriteFramesRead {
    data object Reading : SpriteFramesRead

    data object Unreadable : SpriteFramesRead

    data class Read(val frames: SpriteFrames) : SpriteFramesRead
}
