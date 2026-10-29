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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlin.math.roundToInt

/**
 * An Actor of a block-built type drawn by Kubriko Creator from the type's [look] and the Actor's saved [state], as the
 * type's generated class draws it, for while a change to the type is being compiled. It is selected, moved, and
 * inspected like the Actor it stands for.
 *
 * @param sprite The look's sprite, or null while it is not loaded (nothing is drawn then, as in the game).
 */
internal class PreviewActor(
    private val state: String,
    private val look: ActorTypeLook,
    private val sprite: ImageBitmap? = null,
) : Editable<PreviewActor>, Visible {
    private val bodyObject = SceneDocument.stateObject(state)?.get("body") as? JsonObject
    private val size = bodyObject.pair("size", "width", "height")?.let { (width, height) -> SceneSize(width.sceneUnit, height.sceneUnit) }
        ?: SceneSize(look.width.sceneUnit, look.height.sceneUnit)
    override val body = BoxBody(
        initialPosition = bodyObject.pair("position", "x", "y")?.let { (x, y) -> SceneOffset(x.sceneUnit, y.sceneUnit) } ?: SceneOffset.Zero,
        initialSize = size,
        initialPivot = bodyObject.pair("pivot", "x", "y")?.let { (x, y) -> SceneOffset(x.sceneUnit, y.sceneUnit) } ?: size.center,
        initialScale = bodyObject.pair("scale", "horizontal", "vertical")?.let { (horizontal, vertical) -> Scale(horizontal, vertical) } ?: Scale.Unit,
        initialRotation = ((bodyObject?.get("rotation") as? JsonPrimitive)?.floatOrNull ?: 0f).rad,
    )
    val color = Color(ActorStateEdits.colorOf(state) ?: look.color)

    override fun DrawScope.draw() {
        when {
            look.spriteFile != null -> sprite?.let {
                drawImage(
                    image = it,
                    dstSize = IntSize(body.size.width.raw.roundToInt(), body.size.height.raw.roundToInt()),
                    filterQuality = FilterQuality.None,
                )
            }
            look.shape == ActorShape.CIRCLE -> drawCircle(color = color, radius = body.size.width.raw / 2, center = body.size.center.raw)
            else -> drawRect(color = color, size = body.size.raw)
        }
    }

    override fun save(): Serializable.State<PreviewActor> = PreviewState(
        ActorStateEdits.withChanges(state, ActorStateEdits.Changes(x = body.position.x.raw, y = body.position.y.raw)) ?: state,
        look,
        sprite,
    )

    private class PreviewState(
        private val state: String,
        private val look: ActorTypeLook,
        private val sprite: ImageBitmap?,
    ) : Serializable.State<PreviewActor> {
        override fun restore() = PreviewActor(state, look, sprite)

        override fun serialize() = state
    }

    private fun JsonObject?.pair(key: String, first: String, second: String): Pair<Float, Float>? {
        val values = this?.get(key) as? JsonObject ?: return null
        val firstValue = (values[first] as? JsonPrimitive)?.floatOrNull ?: return null
        val secondValue = (values[second] as? JsonPrimitive)?.floatOrNull ?: return null
        return firstValue to secondValue
    }
}
