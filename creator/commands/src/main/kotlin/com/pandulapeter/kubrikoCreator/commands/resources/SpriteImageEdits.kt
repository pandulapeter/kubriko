/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.ReplaceFiles
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.SpriteFrames
import com.pandulapeter.kubrikoCreator.sprites.sheetOfSprite
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The image file of the sprite at [path] (from `composeResources`): the one the last scan found, or where a new one goes
 * in the game's own module.
 */
internal fun CommandContext.spriteImageFile(path: String): File = spriteFiles()?.files?.get(path) ?: File(scenes.resourcesDirectory, path)

/**
 * The frames of the sprite at [path], cut as its sheet says (one sheet that no longer fits the image is ignored, so the
 * whole image is one frame), read off the main thread; null when its image can't be read.
 */
internal suspend fun CommandContext.readSpriteFrames(path: String): SpriteFrames? = withContext(ioDispatcher) {
    val image = try {
        PixelImage.of(spriteImageFile(path).readBytes())
    } catch (_: IOException) {
        null
    } ?: return@withContext null
    val sheet = spriteFolders.state.value.sheets.sheetOfSprite(path)?.takeIf { it.problemWithImage(image.width, image.height) == null }
    SpriteFrames.of(image, sheet)
}

/**
 * Why frame [frame] of the sprite at [path] can't be changed: there's no such frame in its sheet (one without a sheet is
 * frame 0). Reads nothing.
 */
internal fun CommandContext.validateFrame(path: String, frame: Int?): Rejection? {
    val frameCount = spriteFolders.state.value.sheets.sheetOfSprite(path)?.frameCount ?: 1
    return if (frame == null || frame in 0 until frameCount) null else Rejection(CommandProblem.SPRITE_FRAME_MISSING, path, frame.toString(), (frameCount - 1).toString())
}

/**
 * Changes the image of the sprite at [path] as [edit] says, given its frames: [edit] returns the frames to write, or a
 * rejection when it can't make the change. The image (and the sheet, when the frames changed size) is written through
 * [ReplaceFiles], whose inverse brings both back byte for byte.
 */
@OptIn(EditorMutation::class)
internal suspend fun CommandContext.editSpriteImage(path: String, edit: (SpriteFrames) -> SpriteEdit): Applied {
    val frames = readSpriteFrames(path) ?: return Applied.failed(Rejection(CommandProblem.SPRITE_UNREADABLE, path))
    val edited = when (val result = edit(frames)) {
        is SpriteEdit.Rejected -> return Applied.failed(result.rejection)
        is SpriteEdit.Edited -> result.frames
    }
    val name = ResourceKind.nameOf(path)
    val imagePath = projectPathOf(spriteImageFile(path))
    return try {
        withContext(ioDispatcher) {
            val sheetFiles = if (edited.sheet != frames.sheet) spriteFolders.filesOf(name).associate { projectPathOf(it) to it.readBytes() } else emptyMap()
            val previousImage = (ReplaceFiles.apply(this@editSpriteImage, ReplaceFiles.Input(mapOf(imagePath to edited.toImage().toPng()))).inverse?.input as ReplaceFiles.Input).contents
            val previousSheet = if (edited.sheet != frames.sheet) {
                try {
                    spriteFolders.setSheet(name, edited.sheet)
                } catch (exception: IOException) {
                    // The frames changed size, so the image is put back rather than left cut by the old sheet.
                    ReplaceFiles.apply(this@editSpriteImage, ReplaceFiles.Input(previousImage))
                    throw exception
                }
                sheetFiles + spriteFolders.filesOf(name).map { projectPathOf(it) }.filter { it !in sheetFiles }.associateWith { null }
            } else {
                emptyMap()
            }
            Applied(inverse = Invocation(ReplaceFiles, ReplaceFiles.Input(previousImage + previousSheet)))
        }
    } catch (exception: IOException) {
        Applied.failed(Rejection(CommandProblem.SPRITE_NOT_WRITTEN, path, exception.message ?: exception.toString()))
    }
}

/**
 * What an edit of a sprite's image ([editSpriteImage]) comes to.
 */
internal sealed interface SpriteEdit {
    class Edited(val frames: SpriteFrames) : SpriteEdit

    class Rejected(val rejection: Rejection) : SpriteEdit
}
