/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition

/**
 * Which of the Actor types a generated class does the work of stay inside the Scene's bounds
 * ([ActorTypeDefinition.staysInsideScene]): only a family can have [SOME], whose base class then asks each Actor.
 */
internal enum class SceneContainment {
    NONE,
    SOME,
    ALL,
    ;

    companion object {
        fun of(types: Collection<ActorTypeDefinition>) = when {
            types.none { it.staysInsideScene } -> NONE
            types.all { it.staysInsideScene } -> ALL
            else -> SOME
        }
    }
}
