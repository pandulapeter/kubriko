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
internal sealed interface BlockTypeDefinition {

    /**
     * Also the name of the generated Kotlin class.
     */
    val name: String
    val properties: List<PropertyDefinition>
    val events: List<EventDefinition>
    val kind: BlockTypeKind

    fun withName(name: String): BlockTypeDefinition

    fun withProperties(properties: List<PropertyDefinition>): BlockTypeDefinition

    fun withEvents(events: List<EventDefinition>): BlockTypeDefinition
}
