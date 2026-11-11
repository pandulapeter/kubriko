/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

/**
 * Descriptions many command inputs share, so they read the same in every tool schema.
 */
internal object Descriptions {
    const val DELETE_CLEANUP = "Blocks, Events, and Scene Actors needing it go too; check_change lists what."
    const val TYPE_NAME = "The name of the Actor type or Manager type."
    const val SCENE_NAME = "The Scene: its path in the Scenes folder without .json, like levels/1-1."
    const val EVENT_INDEX = "The Event's index in the type's Event list, from 0."
    const val BLOCK_INDEX = "The index of the Block in the Event, starting from 0."
    const val SCENE_ACTOR_ID = "The Actor's ID, as read_scene returns it."
    const val MANAGER_TYPE_NAME = "The name of the Manager type."
    const val ACTOR_TYPE_NAME = "The name of the Actor type."
}
