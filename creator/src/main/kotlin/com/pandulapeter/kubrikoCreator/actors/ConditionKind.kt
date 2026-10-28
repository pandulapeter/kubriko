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

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.condition_chance
import kubriko.creator.generated.resources.condition_key_held
import kubriko.creator.generated.resources.condition_number
import kubriko.creator.generated.resources.condition_paused
import kubriko.creator.generated.resources.condition_text
import kubriko.creator.generated.resources.condition_toggle
import kubriko.creator.generated.resources.condition_velocity
import org.jetbrains.compose.resources.StringResource

/**
 * The kinds of condition an if Block can check, in the order the editor offers them.
 */
internal enum class ConditionKind(
    val title: StringResource,
    val createDefault: () -> ConditionDefinition,
    val needsActor: Boolean = false,
) {
    NUMBER(Res.string.condition_number, { ConditionDefinition.Number() }),
    TEXT(Res.string.condition_text, { ConditionDefinition.Text() }),
    TOGGLE(Res.string.condition_toggle, { ConditionDefinition.Toggle() }),
    KEY_HELD(Res.string.condition_key_held, { ConditionDefinition.KeyHeld() }),
    VELOCITY(Res.string.condition_velocity, { ConditionDefinition.Velocity() }, needsActor = true),
    CHANCE(Res.string.condition_chance, { ConditionDefinition.Chance() }),
    PAUSED(Res.string.condition_paused, { ConditionDefinition.Paused() });

    fun isAvailableIn(kind: BlockTypeKind) = !needsActor || kind == BlockTypeKind.ACTOR

    companion object {

        fun of(condition: ConditionDefinition) = when (condition) {
            is ConditionDefinition.Number -> NUMBER
            is ConditionDefinition.Text -> TEXT
            is ConditionDefinition.Toggle -> TOGGLE
            is ConditionDefinition.KeyHeld -> KEY_HELD
            is ConditionDefinition.Velocity -> VELOCITY
            is ConditionDefinition.Chance -> CHANCE
            is ConditionDefinition.Paused -> PAUSED
        }
    }
}
