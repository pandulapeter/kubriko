/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

/**
 * Where a [Usage] is: somewhere in Kubriko Creator's model of the game, a hand-written Kotlin file, or the game's
 * settings.
 */
public sealed interface UsagePlace {

    public data class InModel(
        val place: RelationPlace,
    ) : UsagePlace {
        override fun toString(): String = place.toString()
    }

    /**
     * @param path The file's path from the project's root, with `/` between folders.
     */
    public data class KotlinFile(
        val path: String,
    ) : UsagePlace {
        override fun toString(): String = path
    }

    /**
     * A page of the screenplay, by its path from the project's root.
     */
    public data class Markdown(
        val path: String,
    ) : UsagePlace {
        override fun toString(): String = path
    }

    /**
     * A sprite, sound, or music file, by its path from `composeResources`.
     */
    public data class ResourceFile(
        val path: String,
    ) : UsagePlace {
        override fun toString(): String = path
    }

    public data object GameSettings : UsagePlace {
        override fun toString(): String = "The game's settings"
    }
}
