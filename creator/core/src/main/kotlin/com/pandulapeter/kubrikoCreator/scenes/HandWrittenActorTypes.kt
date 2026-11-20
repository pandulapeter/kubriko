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

/**
 * The Actor types a project's own classes register for its Scenes, as far as they are loaded: how an Actor of a type
 * written by hand, whose state only its class knows, is placed.
 */
public interface HandWrittenActorTypes {
    /**
     * The type IDs the loaded classes register (block-built ones and Creator's own among them), none while none are
     * loaded.
     */
    public val typeIds: Set<String>

    /**
     * The saved state of a new Actor of [typeId] at [x], [y], as its metadata creates it, or null if the loaded classes
     * don't register it or creating it failed. It runs the project's code, so it is called off the main thread.
     */
    public fun newState(typeId: String, x: Float, y: Float): String?

    public companion object {
        /**
         * No classes loaded.
         */
        public val None: HandWrittenActorTypes = object : HandWrittenActorTypes {
            override val typeIds: Set<String> = emptySet()

            override fun newState(typeId: String, x: Float, y: Float): String? = null
        }
    }
}
