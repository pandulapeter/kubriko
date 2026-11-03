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
 * What a relation refers to: an Actor type or Manager type, one of their Properties, or a Scene.
 */
public sealed interface RelationSubject {

    public data class Type(
        val name: String,
    ) : RelationSubject {
        override fun toString(): String = name
    }

    /**
     * @param owner The type that declares the Property.
     */
    public data class Property(
        val owner: String,
        val name: String,
    ) : RelationSubject {
        override fun toString(): String = "$owner.$name"
    }

    public data class Scene(
        val name: String,
    ) : RelationSubject {
        override fun toString(): String = "Scene $name"
    }
}
