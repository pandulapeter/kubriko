/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * The kinds of condition an if Block can check, in the order the editor offers them.
 */
public enum class ConditionKind(
    public val createDefault: () -> ConditionDefinition,
    internal val needsActor: Boolean = false,
) {
    NUMBER({ ConditionDefinition.Number() }),
    TEXT({ ConditionDefinition.Text() }),
    TOGGLE({ ConditionDefinition.Toggle() }),
    KEY_HELD({ ConditionDefinition.KeyHeld() }),
    VELOCITY({ ConditionDefinition.Velocity() }, needsActor = true),
    ACTOR_COUNT({ ConditionDefinition.ActorCount() }),
    CHANCE({ ConditionDefinition.Chance() }),
    PAUSED({ ConditionDefinition.Paused() });

    public fun isAvailableIn(kind: BlockTypeKind): Boolean = !needsActor || kind == BlockTypeKind.ACTOR

    public companion object {

        public fun of(condition: ConditionDefinition): ConditionKind = when (condition) {
            is ConditionDefinition.Number -> NUMBER
            is ConditionDefinition.Text -> TEXT
            is ConditionDefinition.Toggle -> TOGGLE
            is ConditionDefinition.KeyHeld -> KEY_HELD
            is ConditionDefinition.Velocity -> VELOCITY
            is ConditionDefinition.ActorCount -> ACTOR_COUNT
            is ConditionDefinition.Chance -> CHANCE
            is ConditionDefinition.Paused -> PAUSED
        }
    }
}
