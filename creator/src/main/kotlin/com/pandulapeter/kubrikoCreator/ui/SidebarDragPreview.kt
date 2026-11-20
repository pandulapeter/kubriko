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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.ui.actorTypes.SpriteChoices
import com.pandulapeter.kubrikoCreator.ui.actorTypes.SpriteThumbnail
import com.pandulapeter.kubrikoCreator.ui.components.DragPreviewChip
import com.pandulapeter.kubrikoCreator.ui.components.rememberImageFile
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_file
import kubriko.creator.generated.resources.ic_folder_open
import java.io.File

/**
 * What is being dragged from the sidebar, following the pointer, with a thumbnail of what it looks like for an Actor type
 * among [blockTypes] or a sprite of the game of [context] ([resources]).
 */
@Composable
internal fun SidebarDragPreview(
    state: SidebarDragState,
    blockTypes: List<BlockTypeDefinition>,
    resources: Map<ResourceGroup, List<ProjectResource>>,
    context: CommandContext,
) {
    val dragged = state.dragged ?: return
    val sheets = context.spriteFolders.state.collectAsState().value.sheets
    val sprites = remember(resources, sheets) { SpriteChoices(GameResources.of(context).spritesAmong(SpriteFiles.of(resources)), sheets) }
    val actorTypes = remember(blockTypes) { blockTypes.filterIsInstance<ActorTypeDefinition>() }
    val look = remember(dragged, actorTypes, sprites) { draggedLookOf(dragged, actorTypes, sprites) }
    DragPreviewChip(
        pointer = { state.pointer },
        icon = when {
            dragged.isFolder -> Res.drawable.ic_folder_open
            else -> dragged.group?.icon ?: Res.drawable.ic_file
        },
        tint = MaterialTheme.colorScheme.tertiary,
        text = if (dragged.isFolder || dragged.group?.resourceKind != null) FolderPaths.nameOf(dragged.name) else dragged.name,
        thumbnail = look?.let { { DraggedLookThumbnail(it) } },
    )
}

/**
 * What something dragged from the sidebar looks like: a sprite (its first frame when cut into frames), or an Actor
 * type's shape in its color.
 */
internal sealed interface DraggedLook {
    data class Sprite(val file: File, val sheet: SpriteSheet?, val tint: Color? = null) : DraggedLook

    data class Shape(val shape: ActorShape, val color: Color) : DraggedLook
}

/**
 * How [dragged] looks, from [actorTypes] and [sprites]: a sprite as itself, an Actor type as its sprite (tinted if it is)
 * or its shape (also when its sprite's file isn't among [sprites]), and anything else, a folder, or a sprite whose file
 * isn't there as nothing, which shows its icon.
 */
internal fun draggedLookOf(dragged: SidebarDragState.Dragged, actorTypes: List<ActorTypeDefinition>, sprites: SpriteChoices): DraggedLook? = when {
    dragged.isFolder -> null
    dragged.group == FolderGroup.SPRITES -> sprites.fileOf(dragged.name)?.let { DraggedLook.Sprite(it, sprites.sheetOf(dragged.name)) }
    dragged.group == FolderGroup.ACTOR_TYPES -> actorTypes.firstOrNull { it.name == dragged.name }?.let { type ->
        val sprite = type.sprite
        val file = sprite?.let(sprites::fileOf)
        if (sprite != null && file != null) {
            DraggedLook.Sprite(file, sprites.sheetOf(sprite), Color(type.color).takeIf { type.tintsSprite })
        } else {
            DraggedLook.Shape(type.shape, Color(type.color))
        }
    }
    else -> null
}

@Composable
internal fun DraggedLookThumbnail(look: DraggedLook) = when (look) {
    is DraggedLook.Sprite -> SpriteThumbnail(
        modifier = Modifier.fillMaxSize(),
        bitmap = rememberImageFile(look.file),
        sheet = look.sheet,
        frame = { 0 },
        tint = look.tint,
    )
    is DraggedLook.Shape -> Canvas(modifier = Modifier.fillMaxSize()) {
        when (look.shape) {
            ActorShape.CIRCLE -> drawCircle(look.color)
            ActorShape.RECTANGLE -> drawRect(look.color, topLeft = Offset(size.width * 0.1f, size.height * 0.2f), size = Size(size.width * 0.8f, size.height * 0.6f))
        }
    }
}
