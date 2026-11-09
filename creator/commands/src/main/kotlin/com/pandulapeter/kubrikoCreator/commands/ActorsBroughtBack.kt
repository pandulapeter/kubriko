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

import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry

/**
 * The Actors a change put back into the Scene called [scene], as they were put in (an undone removal brings them back).
 */
public data class ActorsBroughtBack(
    val scene: String,
    val actors: List<SceneActorEntry>,
) {
    internal companion object {

        /**
         * What several parts of one change brought back, together (each Actor once, as the last part brought it back), if
         * they brought Actors back into one Scene only. A later part may have removed one again, which the editor checks.
         */
        fun merged(parts: List<ActorsBroughtBack>): ActorsBroughtBack? = parts.firstOrNull()?.scene
            ?.takeIf { scene -> parts.all { it.scene == scene } }
            ?.let { scene -> ActorsBroughtBack(scene, parts.flatMap { it.actors }.asReversed().distinctBy { it.id }.asReversed()) }
    }
}
