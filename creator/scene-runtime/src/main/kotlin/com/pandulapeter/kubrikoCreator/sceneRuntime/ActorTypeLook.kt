/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import java.io.File

/**
 * How a block-built Actor type's Actors look, which is known without compiling it: what the Scene editor draws them
 * with ([PreviewActor]) while a change to the type is being compiled.
 *
 * @param color As `0xAARRGGBB`, for Actors whose state has no color of their own.
 * @param width The size of an Actor whose state has none, as a newly placed one gets it.
 * @param isSprite Whether the type is drawn as a sprite rather than its shape, as its generated class decides it (a
 *   sprite with a resource accessor), whether or not its image is there.
 * @param spriteFile The sprite's image, or null while it isn't there (the Actors draw nothing then, as in the game).
 */
public data class ActorTypeLook(
    val shape: ActorShape,
    val color: Long,
    val width: Float,
    val height: Float,
    val spriteFile: File? = null,
    val isSprite: Boolean = spriteFile != null,
) {
    public companion object {

        /**
         * The looks of [actorTypes] by their names, with sprites found among [spriteFiles] (by their path).
         */
        public fun of(actorTypes: List<ActorTypeDefinition>, spriteFiles: Map<String, File>): Map<String, ActorTypeLook> = actorTypes.associate { type ->
            type.name to ActorTypeLook(
                shape = type.shape,
                color = type.color,
                width = type.width,
                height = type.height,
                spriteFile = type.sprite?.let(spriteFiles::get),
                isSprite = type.sprite?.let(SpriteFiles::accessorOf) != null,
            )
        }

        /**
         * The looks in [current] that the classes built from [built] don't draw yet: the changed ones and the new ones.
         */
        internal fun previewsOf(current: Map<String, ActorTypeLook>, built: Map<String, ActorTypeLook>) = current.filter { (name, look) -> built[name] != look }
    }
}
