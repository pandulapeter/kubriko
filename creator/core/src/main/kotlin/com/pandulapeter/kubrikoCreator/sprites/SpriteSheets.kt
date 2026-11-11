/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles

/**
 * How [definition]'s sprite is cut into frames, from sheets by their sprites' names, if it is drawn as one that is.
 */
public fun Map<String, SpriteSheet>.sheetOf(definition: ActorTypeDefinition): SpriteSheet? = definition.sprite?.let(::sheetOfSprite)

/**
 * How the image at [path] (from `composeResources`) is cut into frames, from sheets by their sprites' names, if it is one
 * that is. Only an image right in `drawable` can be: sprites are named after their files, which is unique only there,
 * as their resource accessors are.
 */
public fun Map<String, SpriteSheet>.sheetOfSprite(path: String): SpriteSheet? = path
    .takeIf { SpriteFiles.accessorOf(it) != null }
    ?.let { get(ResourceKind.nameOf(it)) }
