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
internal interface ActorTypeRepository {

    fun load(): List<ActorTypeDefinition>

    fun save(definition: ActorTypeDefinition)

    fun delete(name: String)

    /**
     * Rewrites every Actor type's generated code, so that it matches what this version of Kubriko Creator generates.
     */
    fun regenerateAll()
}
