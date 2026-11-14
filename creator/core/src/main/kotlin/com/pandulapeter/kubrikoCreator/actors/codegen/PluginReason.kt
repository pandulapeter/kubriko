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

/**
 * What in the game needs a plugin ([PluginUse]).
 */
public enum class PluginReason {
    /**
     * An Actor type's collision Event with another Actor type.
     */
    COLLISION_EVENT,

    /**
     * A key pressed Event.
     */
    KEY_PRESSED_EVENT,

    /**
     * An if Block checking whether a key is held.
     */
    KEY_HELD_CONDITION,

    /**
     * A Move with keys Block.
     */
    MOVES_WITH_KEYS,

    /**
     * A Block playing a sound.
     */
    PLAYS_SOUND,

    /**
     * A Block playing music.
     */
    PLAYS_MUSIC,

    /**
     * A Block stopping the music.
     */
    STOPS_MUSIC,

    /**
     * A Scene's music.
     */
    SCENE_MUSIC,

    /**
     * An Actor type drawn as a sprite.
     */
    SPRITE,
}
