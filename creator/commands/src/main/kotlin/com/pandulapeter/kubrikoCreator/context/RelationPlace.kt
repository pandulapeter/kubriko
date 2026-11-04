/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

/**
 * Where a relation is made: a type itself (its parent), one of its Events or Blocks, a text or the Kotlin UI of a
 * Manager type, or a Scene. Written as tool results address it, with indexes from 0: `Ball#2.1` is Block 1 of
 * Event 2 of Ball.
 */
public sealed interface RelationPlace {

    /**
     * The type or Scene the place belongs to.
     */
    public val owner: RelationSubject

    public data class Type(
        val name: String,
        val eventIndex: Int? = null,
        val blockIndex: Int? = null,
    ) : RelationPlace {
        override val owner: RelationSubject get() = RelationSubject.Type(name)

        override fun toString(): String = when {
            eventIndex == null -> name
            blockIndex == null -> "$name#$eventIndex"
            else -> "$name#$eventIndex.$blockIndex"
        }
    }

    public data class Text(
        val manager: String,
        val textIndex: Int,
    ) : RelationPlace {
        override val owner: RelationSubject get() = RelationSubject.Type(manager)

        override fun toString(): String = "$manager text $textIndex"
    }

    public data class KotlinUi(
        val manager: String,
    ) : RelationPlace {
        override val owner: RelationSubject get() = RelationSubject.Type(manager)

        override fun toString(): String = "$manager Kotlin UI"
    }

    public data class Scene(
        val name: String,
    ) : RelationPlace {
        override val owner: RelationSubject get() = RelationSubject.Scene(name)

        override fun toString(): String = "Scene $name"
    }
}
