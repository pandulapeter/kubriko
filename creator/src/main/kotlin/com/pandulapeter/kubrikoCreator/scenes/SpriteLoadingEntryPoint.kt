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

import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sprites.SpriteManager

/**
 * A project's [entryPoint] for the Scene editor in Kubriko Creator's own window, whose Managers draw the project's
 * sprites there: the project's `SpriteManager` is replaced by one that reads them with [loader].
 */
internal class SpriteLoadingEntryPoint(
    private val entryPoint: CreatorEntryPoint,
    private val loader: ProjectSpriteLoader,
) : CreatorEntryPoint by entryPoint {

    override fun createCustomManagers(): List<Manager> = entryPoint.createCustomManagers().map { manager ->
        if (manager is SpriteManager) SpriteManager.newInstance(imageLoader = loader::load) else manager
    }
}
