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
 * Where a workspace's block-built Actor types are kept.
 */
internal interface BlockTypeRepository {

    fun load(): List<ActorTypeDefinition>

    /**
     * @return The Actor types whose generated code was changed by hand, and so wasn't updated.
     */
    fun save(definition: ActorTypeDefinition): Set<String>

    /**
     * @return The Actor types whose generated code was changed by hand, and so wasn't updated.
     */
    fun delete(name: String): Set<String>

    /**
     * Rewrites every Actor type's generated code, so that it matches what this version of Kubriko Creator generates,
     * except where it was changed by hand, unless that Actor type is in [overwriting].
     *
     * @return The Actor types whose generated code was changed by hand, and so wasn't updated.
     */
    fun regenerateAll(overwriting: Set<String> = emptySet()): Set<String>
}
