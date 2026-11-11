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
 * Something about the game worth a look, found from Kubriko Creator's model of it ([HealthChecks]): not necessarily a
 * mistake, but often one.
 *
 * @param subject What it is about: an Actor type, a Property, a Scene, a page, or an image.
 * @param detail The Timer or the other Actor type it names, the place of the Block (`Ball#1.2`), or the Scene the game
 *   starts with, for the kinds that name one.
 * @param usage What a Kotlin Block uses that changed (`return`, or the name of a local it no longer has), or what a
 *   reference names that no longer exists.
 */
public data class HealthFinding(
    val kind: Kind,
    val subject: RelationSubject,
    val detail: String? = null,
    val usage: String? = null,
) {
    public enum class Kind {
        /**
         * An Actor type that no Scene places and no Block adds, nor any type based on it.
         */
        UNUSED_ACTOR_TYPE,

        /**
         * A Scene no goToScene Block goes to, other than the one the game starts with: [detail], when the project's
         * settings name one that exists, and without it, any one of them may be.
         */
        UNREACHED_SCENE,

        /**
         * The Scene the game starts with ([subject], as the project's settings name it), which doesn't exist.
         */
        FIRST_SCENE_MISSING,

        /**
         * A Property nothing reads: no condition, Log Block, text, or Kotlin names it.
         */
        UNREAD_PROPERTY,

        /**
         * A timerFinished Event whose Timer no startTimer Block of the type's family starts, so it never fires.
         */
        TIMER_NEVER_STARTED,

        /**
         * A collision Event with an Actor type that is never there, so it never fires.
         */
        COLLISION_WITH_ABSENT_TYPE,

        /**
         * A Kotlin Block that uses `return` or a local its Event no longer has, now that each kind of Event runs in a
         * function of its own ([KotlinBlockScopeChecks]).
         */
        KOTLIN_BLOCK_SCOPE_CHANGED,

        /**
         * A Block, Event, placeholder, or parent of [subject] naming [usage], which no longer exists
         * ([MissingReferenceChecks]), at the place in [detail].
         */
        REFERENCE_MISSING,

        /**
         * An image among the pages that no page or document shows or links to ([PageImageChecks]).
         */
        UNUSED_PAGE_IMAGE,

        /**
         * A page showing the image at [usage], whose file is gone ([PageImageChecks]).
         */
        PAGE_IMAGE_MISSING,

        /**
         * An Actor type, Manager type, or Scene no page of the screenplay mentions ([ScreenplayChecks]).
         */
        UNMENTIONED,

        /**
         * A page mentioning the file at [usage], which is gone ([ScreenplayChecks]).
         */
        MENTION_MISSING,

        /**
         * A page without its `#` title ([ScreenplayChecks]).
         */
        PAGE_WITHOUT_TITLE,

        /**
         * A page without its one-sentence summary under its title ([ScreenplayChecks]).
         */
        PAGE_WITHOUT_SUMMARY,

        /**
         * A Planned item of a page, [detail], mentioning what already exists, so it may be built ([ScreenplayChecks]).
         */
        PLANNED_MAY_BE_BUILT,
    }

    override fun toString(): String = when (kind) {
        Kind.UNUSED_ACTOR_TYPE -> "$subject: no Scene places it and no Block adds it, nor any type based on it."
        Kind.UNREACHED_SCENE -> if (detail == null) {
            "$subject: no goToScene Block goes to it, which is fine if the game starts with it."
        } else {
            "$subject: no goToScene Block goes to it, and the game starts with $detail."
        }
        Kind.FIRST_SCENE_MISSING -> "$subject: the game starts with it (game.firstScene in gradle.properties), but it doesn't exist."
        Kind.UNREAD_PROPERTY -> "$subject: nothing reads it (no condition, Log Block, text, or Kotlin names it)."
        Kind.TIMER_NEVER_STARTED -> "$subject: the timerFinished Event of $detail never fires, since no startTimer Block of its family starts it."
        Kind.COLLISION_WITH_ABSENT_TYPE -> "$subject: the collision Event with $detail never fires, since no Scene places $detail and no Block adds it."
        Kind.REFERENCE_MISSING -> "$detail: names $usage, which no longer exists."
        Kind.UNUSED_PAGE_IMAGE -> "$subject: no page or document shows it."
        Kind.PAGE_IMAGE_MISSING -> "$subject: shows $usage, which is gone."
        Kind.UNMENTIONED -> "$subject: no page of the screenplay mentions it."
        Kind.MENTION_MISSING -> "$subject: mentions $usage, which is gone."
        Kind.PAGE_WITHOUT_TITLE -> "$subject: has no # title."
        Kind.PAGE_WITHOUT_SUMMARY -> "$subject: has no one-sentence summary under its title."
        Kind.PLANNED_MAY_BE_BUILT -> "$subject: the Planned item \"$detail\" mentions what already exists, so it may be built."
        Kind.KOTLIN_BLOCK_SCOPE_CHANGED -> if (usage == KotlinBlockScopeChecks.RETURN) {
            "$detail: return in this Kotlin Block now ends only this Event's Blocks (and those of the type's later Events of the same kind), not the rest of the frame or the other collisions."
        } else {
            "$detail: this Kotlin Block uses $usage, which its Event no longer has; read it from body or kubriko instead."
        }
    }
}
