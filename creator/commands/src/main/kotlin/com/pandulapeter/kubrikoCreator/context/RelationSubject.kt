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
 * What a relation refers to: an Actor type or Manager type, one of their Properties, a Scene, a page of the screenplay,
 * a Timer, a sprite, sound, or music file, or an image next to the pages.
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

    /**
     * A page of the screenplay, by its name (`levels/cave`).
     */
    public data class Page(
        val name: String,
    ) : RelationSubject {
        override fun toString(): String = "Page $name"
    }

    /**
     * A Timer, by the type it was asked about through ([owner]): the Timers of an Actor type are its whole family's.
     */
    public data class Timer(
        val owner: String,
        val name: String,
    ) : RelationSubject {
        override fun toString(): String = "Timer $name of $owner"
    }

    /**
     * A sprite, sound, or music file, by its path from `composeResources` (`drawable/ball.png`).
     */
    public data class Resource(
        val path: String,
    ) : RelationSubject {
        override fun toString(): String = path
    }

    /**
     * An image file among the pages, by its path from the project's root.
     */
    public data class Image(
        val path: String,
    ) : RelationSubject {
        override fun toString(): String = path
    }
}
