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

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubrikoCreator.scenes.HandWrittenActorTypes

/**
 * The Actor types of the project's classes the Scene editor loaded last, until they are released: what placing an Actor
 * of a hand-written type reads its new state from. What the project's code throws (a `TODO()` too), or a class it can't
 * link, counts as no type and no state.
 */
public class LoadedActorTypes : HandWrittenActorTypes {
    @Volatile
    private var current: Current? = null

    private class Current(
        val loaded: LoadedEntryPoint,
        val serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
        val typeIds: Set<String>,
    )

    override val typeIds: Set<String> get() = current?.typeIds.orEmpty()

    override fun newState(typeId: String, x: Float, y: Float): String? = runProjectCode {
        current?.serializationManager?.getMetadata(typeId)?.instantiate(SceneOffset(x.sceneUnit, y.sceneUnit))?.serialize()
    }

    /**
     * Reads the types of [loaded], which are used from now on.
     */
    internal fun update(loaded: LoadedEntryPoint) {
        current = runProjectCode {
            val serializationManager = loaded.entryPoint.createSerializationManager()
            Current(loaded, serializationManager, serializationManager.registeredTypeIds.toSet())
        }
    }

    /**
     * Stops using [loaded], which is about to be closed, if it is the one used.
     */
    internal fun release(loaded: LoadedEntryPoint) {
        if (current?.loaded === loaded) current = null
    }

    private inline fun <T> runProjectCode(block: () -> T): T? = try {
        block()
    } catch (_: Exception) {
        null
    } catch (_: LinkageError) {
        null
    } catch (_: NotImplementedError) {
        null
    }
}
