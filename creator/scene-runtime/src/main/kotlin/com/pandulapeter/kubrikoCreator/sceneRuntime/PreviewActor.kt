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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.looks.EllipseLook
import com.pandulapeter.kubriko.actor.looks.Look
import com.pandulapeter.kubriko.actor.looks.RectangleLook
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.actor.traits.Batched
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.scenes.ActorStateEdits
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull

/**
 * An Actor of a block-built type drawn by Kubriko Creator from the type's [typeLook] and the Actor's saved [state], for
 * while a change to the type is being compiled. It is drawn through the same batched [look] as the type's generated class,
 * so the preview and the game always match. It is selected, moved, and inspected like the Actor it stands for.
 *
 * @param sprite The look's sprite, or null while it is not loaded (nothing is drawn then, as in the game).
 */
internal class PreviewActor(
    private val state: String,
    private val typeLook: ActorTypeLook,
    private val sprite: ImageBitmap? = null,
) : Editable<PreviewActor>, Batched {
    private val bodyObject = SceneDocument.stateObject(state)?.get("body") as? JsonObject
    private val size = bodyObject.pair("size", "width", "height")?.let { (width, height) -> SceneSize(width.sceneUnit, height.sceneUnit) }
        ?: SceneSize(typeLook.width.sceneUnit, typeLook.height.sceneUnit)
    override val body = BoxBody(
        initialPosition = bodyObject.pair("position", "x", "y")?.let { (x, y) -> SceneOffset(x.sceneUnit, y.sceneUnit) } ?: SceneOffset.Zero,
        initialSize = size,
        initialPivot = bodyObject.pair("pivot", "x", "y")?.let { (x, y) -> SceneOffset(x.sceneUnit, y.sceneUnit) } ?: size.center,
        initialScale = bodyObject.pair("scale", "horizontal", "vertical")?.let { (horizontal, vertical) -> Scale(horizontal, vertical) } ?: Scale.Unit,
        initialRotation = ((bodyObject?.get("rotation") as? JsonPrimitive)?.floatOrNull ?: 0f).rad,
    )
    val color = Color(ActorStateEdits.colorOf(state) ?: typeLook.color)

    /**
     * The look the type's generated class has: its sprite, its pixels kept sharp, or its shape in the Actor's color.
     */
    override val look: Look = when {
        typeLook.spriteFile != null -> SpriteLook(texture = sprite, filterQuality = FilterQuality.None)
        typeLook.shape == ActorShape.CIRCLE -> EllipseLook(color)
        else -> RectangleLook(color)
    }

    override fun save(): Serializable.State<PreviewActor> = PreviewState(
        ActorStateEdits.withChanges(state, ActorStateEdits.Changes(x = body.position.x.raw, y = body.position.y.raw)) ?: state,
        typeLook,
        sprite,
    )

    private class PreviewState(
        private val state: String,
        private val typeLook: ActorTypeLook,
        private val sprite: ImageBitmap?,
    ) : Serializable.State<PreviewActor> {
        override fun restore() = PreviewActor(state, typeLook, sprite)

        override fun serialize() = state
    }

    private fun JsonObject?.pair(key: String, first: String, second: String): Pair<Float, Float>? {
        val values = this?.get(key) as? JsonObject ?: return null
        val firstValue = (values[first] as? JsonPrimitive)?.floatOrNull ?: return null
        val secondValue = (values[second] as? JsonPrimitive)?.floatOrNull ?: return null
        return firstValue to secondValue
    }
}
