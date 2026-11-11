/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.ui.screenplay.PastedImages
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * What the sidebar's sprites, sounds, and music groups do besides their folders' and files' own ([FolderTreeActions]).
 *
 * @param onChoose Asks for files of a kind to add.
 * @param onAdd Adds files chosen, dropped, or pasted, each named after its own name.
 * @param onPaste Adds what the clipboard holds: copied files, or a copied image as a sprite.
 * @param onCreateSprite Makes a new, see-through sprite of a name and size and opens it, unless it is refused, returning
 *   why it was.
 * @param onImportFrames Asks for images to make a new sprite of, one frame each, and opens it.
 */
internal class ResourceActions(
    val onChoose: (ResourceKind) -> Unit = {},
    val onAdd: (List<ResourceSource>) -> Unit = {},
    val onPaste: () -> Unit = {},
    val onCreateSprite: (name: String, width: Int, height: Int) -> Rejection? = { _, _, _ -> null },
    val onImportFrames: () -> Unit = {},
)

/**
 * A file to add as a sprite, sound, or music, named after [name]: its own, or a word for one whose temporary name says
 * nothing, such as a pasted image's.
 */
internal data class ResourceSource(
    val file: File,
    val name: String = file.name,
)

/**
 * The kind of file a group of the sidebar holds, if Kubriko Creator adds, renames, and deletes them.
 */
internal val ResourceGroup.resourceKind: ResourceKind?
    get() = when (this) {
        ResourceGroup.SPRITES -> ResourceKind.SPRITE
        ResourceGroup.SOUNDS -> ResourceKind.SOUND
        ResourceGroup.MUSIC -> ResourceKind.MUSIC
        ResourceGroup.SCENES, ResourceGroup.DOCUMENTS, ResourceGroup.KOTLIN -> null
    }

/**
 * The path of [resource] from its `composeResources` folder, if it is a sprite, sound, or music file Kubriko Creator
 * renames and deletes (not, for example, an image in a qualified `drawable` folder).
 */
internal fun resourcePathOf(resource: ProjectResource): String? =
    resource.relativePath.substringAfterLast("${ResourceKind.COMPOSE_RESOURCES}/", "").takeIf { ResourceKind.of(it) != null }

/**
 * The files among [files] that can be added as a sprite, sound, or music.
 */
internal fun resourceFilesAmong(files: List<File>): List<ResourceSource> = files.filter { it.isFile && ResourceKind.ofExtension(it.extension) != null }.map(::ResourceSource)

/**
 * What the clipboard (the system's by default) holds that can be added: copied files, or a copied image, saved as a
 * PNG named [PASTED_IMAGE_NAME]; nothing when it holds text, since many apps offer the text they copy as an image too
 * (as [com.pandulapeter.kubrikoCreator.ui.screenplay.pastedImagesOf] reads it). Blocks, so it runs off the main thread.
 */
internal fun pastedResources(clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard): List<ResourceSource> = runCatching {
    when {
        clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor) -> resourceFilesAmong((clipboard.getData(DataFlavor.javaFileListFlavor) as List<*>).filterIsInstance<File>())
        clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor) -> emptyList()
        clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor) -> listOfNotNull(PastedImages.Image(clipboard).read()?.let { ResourceSource(it.file, PASTED_IMAGE_NAME) })
        else -> emptyList()
    }
}.getOrDefault(emptyList())

/**
 * Asks the user for files of [kind], returning none if they cancel.
 */
internal fun pickResourceFiles(kind: ResourceKind, title: String): List<ResourceSource> = JFileChooser().run {
    dialogTitle = title
    isMultiSelectionEnabled = true
    fileFilter = FileNameExtensionFilter(title, *kind.extensions.toTypedArray())
    if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) resourceFilesAmong(selectedFiles.toList()) else emptyList()
}

/**
 * Asks the user for images to make the frames of a new sprite of, in the order of their names (`walk-2` before
 * `walk-10`), returning none if they cancel.
 */
internal fun pickFrameFiles(title: String): List<File> = JFileChooser().run {
    dialogTitle = title
    isMultiSelectionEnabled = true
    fileFilter = FileNameExtensionFilter(title, *ResourceKind.SPRITE.extensions.toTypedArray())
    if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selectedFiles.filter { it.isFile }.sortedWith(compareBy(::framesOrderOf)) else emptyList()
}

/**
 * How frame files sort: by their names, with each run of digits as the number it is, so `walk-2` comes before `walk-10`.
 */
internal fun framesOrderOf(file: File): String = file.nameWithoutExtension.lowercase().replace(Regex("[0-9]+")) { it.value.padStart(FRAME_NUMBER_WIDTH, '0') }

/**
 * The name a sprite made of [files] as its frames is offered under: the first one's, without the number that tells the
 * frames apart (`walk-1` becomes `walk`).
 */
internal fun frameSpriteNameOf(files: List<File>): String = files.first().nameWithoutExtension.replace(Regex("[ _-]*[0-9]+$"), "").ifEmpty { files.first().nameWithoutExtension }

private const val FRAME_NUMBER_WIDTH = 10

/**
 * What a pasted image is named after, since its file's name is a temporary one.
 */
internal const val PASTED_IMAGE_NAME = "pasted.png"
