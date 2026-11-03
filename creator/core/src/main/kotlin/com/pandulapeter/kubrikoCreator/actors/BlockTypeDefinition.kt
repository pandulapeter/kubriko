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
 * A type built from Events and Blocks in Kubriko Creator, which generates a Kotlin class from it. Actor types and
 * Manager types share one namespace, since both become classes in the `generated` package.
 */
public sealed interface BlockTypeDefinition {

    /**
     * Also the name of the generated Kotlin class.
     */
    public val name: String

    /**
     * What the type is for in the game, in one line, filled in by the user or the AI.
     */
    public val description: String?
    public val properties: List<PropertyDefinition>
    public val events: List<EventDefinition>
    public val kind: BlockTypeKind

    /**
     * The folder it is in, as a path of folder names (`enemies/flying`), empty for none. Kept as where its definition
     * file is, not in the file.
     */
    public val folder: String

    public fun withName(name: String): BlockTypeDefinition

    public fun withDescription(description: String?): BlockTypeDefinition

    public fun withFolder(folder: String): BlockTypeDefinition

    public fun withProperties(properties: List<PropertyDefinition>): BlockTypeDefinition

    public fun withEvents(events: List<EventDefinition>): BlockTypeDefinition
}
