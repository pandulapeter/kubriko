/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime.leakProbe

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

/**
 * A project's entry point for [com.pandulapeter.kubrikoCreator.scenes.ClassLoaderReleaseTest], which hides this package
 * from the project's class loader's parent so that the project's loader defines these classes itself.
 */
class LeakProbeEntryPoint : CreatorEntryPoint {
    override fun createSerializationManager() = EditableMetadata.newSerializationManagerInstance(
        EditableMetadata(
            typeId = TYPE_ID,
            deserializeState = { LeakProbeState(it) },
            instantiate = { LeakProbeState("${it.x.raw},${it.y.raw}") },
        ),
    )

    override fun createCustomManagers() = listOf(LeakProbeManager())

    companion object {
        const val TYPE_ID = "Probe"
    }
}

class LeakProbeManager : Manager() {
    override fun onInitialize(kubriko: Kubriko) = Unit
}

class LeakProbeActor(position: SceneOffset) : Editable<LeakProbeActor>, Visible {
    override val body = BoxBody(initialPosition = position, initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))

    override fun save() = LeakProbeState("${body.position.x.raw},${body.position.y.raw}")

    override fun DrawScope.draw() = Unit
}

class LeakProbeState(private val json: String) : Serializable.State<LeakProbeActor> {
    override fun restore(): LeakProbeActor {
        val (x, y) = json.split(',').map(String::toFloat)
        return LeakProbeActor(SceneOffset(x.sceneUnit, y.sceneUnit))
    }

    override fun serialize() = json
}
