/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.project.FormatMigration
import java.io.File

/**
 * Format version 7: the When it leaves the Scene Event, and staying inside the Scene as a setting of the Actor type
 * ([ActorTypeDefinition.staysInsideScene]) rather than what the edge Event does. Every Actor type that reacts to the edge
 * Event, its own or inherited, is set to stay inside, so existing games behave the same, after the names of what the
 * generated classes keep about the bounds ([PropertyNaming.SCENE_BOUNDS_NAMES]) are freed as [ReservedPropertyMigration]
 * frees them. Type files that can't be read are left as they are.
 */
internal object SceneLeavingMigration : FormatMigration {

    override fun migrate(rootDirectory: File) {
        ReservedPropertyMigration(*PropertyNaming.SCENE_BOUNDS_NAMES.toTypedArray()).migrate(rootDirectory)
        val store = BlockTypeStore.open(rootDirectory) ?: return
        val actorTypes = store.load(isCheckingPropertyNames = false).types.filterIsInstance<ActorTypeDefinition>()
        val family = ActorTypeFamily(actorTypes)
        actorTypes
            .filter { type -> !type.staysInsideScene && family.eventsOf(type.name).any { it.type == EventType.EDGE_TOUCHED } }
            .forEach { store.save(it.copy(staysInsideScene = true)) }
    }
}
