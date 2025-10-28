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
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.abs
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.max

internal abstract class IsometricRepresentation(
    positionX: SceneUnit = SceneUnit.Zero,
    positionY: SceneUnit = SceneUnit.Zero,
    positionZ: SceneUnit = SceneUnit.Zero,
    dimensionX: SceneUnit = SceneUnit.Zero,
    dimensionY: SceneUnit = SceneUnit.Zero,
    dimensionZ: SceneUnit = SceneUnit.Zero,
    var rotationZ: AngleRadians = AngleRadians.Zero
) : Visible {

    var positionX = positionX
        private set
    var positionY = positionY
        private set
    var positionZ = positionZ
        private set
    var dimensionX = dimensionX
        private set
    var dimensionY = dimensionY
        private set
    var dimensionZ = dimensionZ
        private set
    protected open val extraDepth = 0f

    override val body = BoxBody()
    override val drawingOrder get() = positionX.raw + positionY.raw -0.1f * positionZ.raw + extraDepth
    abstract val tileWidthMultiplier: Float
    abstract val tileHeightMultiplier: Float

    override fun onAdded(kubriko: Kubriko) = update(
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        dimensionX = dimensionX,
        dimensionY = dimensionY,
        dimensionZ = dimensionZ,
    )

    fun update(
        positionX: SceneUnit = this.positionX,
        positionY: SceneUnit = this.positionY,
        positionZ: SceneUnit = this.positionZ,
        dimensionX: SceneUnit = this.dimensionX,
        dimensionY: SceneUnit = this.dimensionY,
        dimensionZ: SceneUnit = this.dimensionZ,
        rotationZ: AngleRadians = this.rotationZ,
    ) {
        val magicEffect = (tileHeightMultiplier / tileWidthMultiplier) * 1.6f
        this.positionX = positionX
        this.positionY = -positionY
        this.positionZ = positionZ / magicEffect
        this.dimensionX = dimensionX
        this.dimensionY = dimensionY
        this.dimensionZ = dimensionZ / magicEffect
        this.rotationZ = rotationZ
        updateIsometricBody()
    }

    // TODO: Cumbersome
    fun updateWithoutProcessing(
        positionX: SceneUnit = this.positionX,
        positionY: SceneUnit = this.positionY,
        positionZ: SceneUnit = this.positionZ,
        dimensionX: SceneUnit = this.dimensionX,
        dimensionY: SceneUnit = this.dimensionY,
        dimensionZ: SceneUnit = this.dimensionZ,
        rotationZ: AngleRadians = this.rotationZ,
    ) {
        this.positionX = positionX
        this.positionY = positionY
        this.positionZ = positionZ
        this.dimensionX = dimensionX
        this.dimensionY = dimensionY
        this.dimensionZ = dimensionZ
        this.rotationZ = rotationZ
        updateIsometricBody()
    }

    open fun updateIsometricBody() {
        body.size = SceneSize(
            width = max(dimensionX.raw, dimensionY.raw).sceneUnit * tileWidthMultiplier * 1.1f,
            height = (dimensionX + dimensionY + dimensionZ) * tileHeightMultiplier
                    + abs(positionZ) * tileHeightMultiplier,
        )
        body.pivot = SceneOffset(
            x = body.size.width * 0.5f,
            y = body.size.height
                    - max(dimensionX.raw, dimensionY.raw).sceneUnit * tileHeightMultiplier * 0.5f
                    - positionZ * tileHeightMultiplier,
        )
        body.position = SceneOffset(
            x = (positionX - positionY) * tileWidthMultiplier * 0.5f,
            y = (-positionX - positionY) * tileHeightMultiplier * 0.5f
                    - positionZ * tileHeightMultiplier,
        )
    }
}