/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.traits

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers.IsometricGraphicsDemoManager
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.types.SceneUnit

internal abstract class VisibleInWorld() : Visible, Dynamic {

    abstract val height: SceneUnit
    abstract val positionZ: SceneUnit
    abstract val isometricRepresentation: IsometricRepresentation
    protected lateinit var isometricGraphicsDemoManager: IsometricGraphicsDemoManager
        private set

    override fun onAdded(kubriko: Kubriko) {
        isometricGraphicsDemoManager = kubriko.get()
        isometricGraphicsDemoManager.isometricWorldActorManager.add(isometricRepresentation)
    }

    override fun onRemoved() {
        isometricGraphicsDemoManager.isometricWorldActorManager.remove(isometricRepresentation)
    }

    override fun update(deltaTimeInMilliseconds: Int) = isometricRepresentation.update(
        positionX = body.position.x,
        positionY = body.position.y,
        positionZ = positionZ,
        dimensionX = body.size.width,
        dimensionY = body.size.height,
        dimensionZ = height,
        rotationZ = body.rotation,
    )
}