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
 * Where a project keeps the definitions of its types, which the store reads and writes and the generated code names in its
 * header.
 */
internal object DefinitionPaths {
    const val CREATOR_DIRECTORY = ".kubriko"

    /**
     * The folder in [CREATOR_DIRECTORY] that holds the definitions of types of [kind].
     */
    fun definitionsFolder(kind: BlockTypeKind) = when (kind) {
        BlockTypeKind.ACTOR -> "actors"
        BlockTypeKind.MANAGER -> "managers"
    }

    /**
     * Where [definition] is saved, relative to the project's folder.
     */
    fun definitionPath(definition: BlockTypeDefinition) = "$CREATOR_DIRECTORY/${definitionsFolder(definition.kind)}/" + "${definition.folder}/${definition.name}.json".removePrefix("/")
}
