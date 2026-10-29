/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import java.io.File

/**
 * How a block-built Actor type's Actors look, which is known without compiling it: what the Scene editor draws them
 * with ([PreviewActor]) while a change to the type is being compiled.
 *
 * @param color As `0xAARRGGBB`, for Actors whose state has no color of their own.
 * @param width The size of an Actor whose state has none, as a newly placed one gets it.
 * @param spriteFile The image drawn instead of the shape, or null for the shape.
 */
internal data class ActorTypeLook(
    val shape: ActorShape,
    val color: Long,
    val width: Float,
    val height: Float,
    val spriteFile: File? = null,
) {
    companion object {

        /**
         * The looks of [actorTypes] by their names, with sprites found among [spriteFiles] (by their path).
         */
        fun of(actorTypes: List<ActorTypeDefinition>, spriteFiles: Map<String, File>) = actorTypes.associate { type ->
            type.name to ActorTypeLook(
                shape = type.shape,
                color = type.color,
                width = type.width,
                height = type.height,
                spriteFile = type.sprite?.let(spriteFiles::get),
            )
        }

        /**
         * The looks in [current] that the classes built from [built] don't draw yet: the changed ones and the new ones.
         */
        fun previewsOf(current: Map<String, ActorTypeLook>, built: Map<String, ActorTypeLook>) = current.filter { (name, look) -> built[name] != look }
    }
}
