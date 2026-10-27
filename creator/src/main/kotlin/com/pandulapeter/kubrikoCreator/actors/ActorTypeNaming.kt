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
import kubriko.creator.generated.resources.error_actor_type_name_invalid
import kubriko.creator.generated.resources.error_actor_type_name_reserved
import kubriko.creator.generated.resources.error_actor_type_name_taken
import org.jetbrains.compose.resources.StringResource

/**
 * The rules for Actor type names, which are also Kotlin class names and Scene type IDs.
 */
internal object ActorTypeNaming {

    private val namePattern = Regex("[A-Z][A-Za-z0-9]*")
    private val reservedNames = setOf(ActorTypeCodeGenerator.REGISTRY_NAME, "State", "Color", "Key", "Kubriko", "SceneOffset", "SceneSize")

    /**
     * Why [name] can't be a new Actor type's name, or null if it can. Names that differ only in case are taken too,
     * since they would clash on case-insensitive file systems.
     */
    fun validate(name: String, existingNames: Collection<String>): StringResource? = when {
        !namePattern.matches(name) -> Res.string.error_actor_type_name_invalid
        name in reservedNames -> Res.string.error_actor_type_name_reserved
        existingNames.any { it.equals(name, ignoreCase = true) } -> Res.string.error_actor_type_name_taken
        else -> null
    }
}
