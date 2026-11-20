/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime.generated

import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * An Actor class in a package named as the one Kubriko Creator generates into, as a block-built type's class is.
 */
class GeneratedBrick : Editable<GeneratedBrick> {
    override val body = PointBody(SceneOffset.Zero)

    override fun save() = object : Serializable.State<GeneratedBrick> {
        override fun restore() = GeneratedBrick()

        override fun serialize() = "{}"
    }
}
