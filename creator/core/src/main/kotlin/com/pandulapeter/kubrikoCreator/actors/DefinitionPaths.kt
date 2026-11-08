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
public object DefinitionPaths {
    public const val CREATOR_DIRECTORY: String = ".kubriko"

    /**
     * The folder in [CREATOR_DIRECTORY] that holds the definitions of types of [kind].
     */
    public fun definitionsFolder(kind: BlockTypeKind): String = when (kind) {
        BlockTypeKind.ACTOR -> "actors"
        BlockTypeKind.MANAGER -> "managers"
    }

    /**
     * Where [definition] is saved, relative to the project's folder.
     */
    public fun definitionPath(definition: BlockTypeDefinition): String = definitionPath(definition.kind, definition.folder, definition.name)

    /**
     * Where a type of [kind] called [name] in [folder] is saved, relative to the project's folder.
     */
    public fun definitionPath(kind: BlockTypeKind, folder: String, name: String): String = "${folderPath(kind, folder)}/$name.json"

    /**
     * Where the folder [folder] of types of [kind] is, relative to the project's folder: the definitions folder itself for
     * an empty path.
     */
    public fun folderPath(kind: BlockTypeKind, folder: String): String = "$CREATOR_DIRECTORY/${definitionsFolder(kind)}" + (if (folder.isEmpty()) "" else "/$folder")
}
