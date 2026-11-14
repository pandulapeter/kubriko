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
 * The rules for the names of Properties, which are the names of their Kotlin properties too.
 */
public object PropertyNaming {

    /**
     * The names of what the generated Actor classes keep about the Scene's bounds, reserved since format version 7.
     */
    internal val SCENE_BOUNDS_NAMES = listOf(
        "staysInsideScene", "wasInsideScene", "wasTouchingLeftEdge", "wasTouchingRightEdge", "wasTouchingTopEdge", "wasTouchingBottomEdge",
    )

    /**
     * Kotlin's keywords, and the names the generated classes (and the Kubriko traits and `Manager` they extend) already use.
     */
    private val reservedNames = KotlinIdentifiers.hardKeywords + setOf(
        "actorManager", "body", "bodyCenter", "bottomRight", "collidableTypes", "collidables", "collisionMask", "color", "deltaTimeInMilliseconds",
        "drawingOrder", "generatedAudio", "generatedScenes", "halfSize", "index", "isAlwaysActive", "keyboardInputManager", "isVisible", "key", "kubriko", "layerIndex", "metadata", "other",
        "shouldClip", "state", "stateManager", "topLeft", "velocity", "viewportManager",
        "box", "isRemoved", "sceneSettings", "sprite", "spriteManager",
        "isInSceneEditor", "isInitialized", "isLoggingEnabled", "scope",
    ) + SCENE_BOUNDS_NAMES

    /**
     * The names only the generated Actor classes use, which the format raises that added them reserved: a Manager type's
     * Property may have them.
     */
    private val actorMemberNames = setOf(
        "look", "animation", "animationSpeed", "isAnimationLooping", "velocityInDirection", "opacity", "velocityFromKeys",
        "sceneChangesWhenAdded", "actorCountGuard",
    ) + AnimationNaming.reservedNames

    /**
     * The names only the generated Manager classes use (the counters of the actorAdded and actorRemoved Events, and the
     * package their class is named in full from), which the format raise that added them reserved: an Actor type's
     * Property may have them.
     */
    private val managerMemberNames = setOf("actorsAdded", "actorsRemoved", "kotlin")

    /**
     * [base], or the first of base2, base3, … that can be the name of a Property of a type of [kind] among [existingNames].
     */
    public fun freeName(base: String, existingNames: Collection<String>, kind: BlockTypeKind): String = generateSequence(1) { it + 1 }
        .map { if (it == 1) base else "$base$it" }
        .first { validate(it, existingNames, kind) == null }

    /**
     * Why [name] can't be the name of a Property of a type of [kind] among [existingNames], or null if it can. Names that differ only in
     * case are taken too, like those of Actor types.
     */
    public fun validate(name: String, existingNames: Collection<String>, kind: BlockTypeKind): NameProblem? = when (NameFlaw.of(name, isFirstAllowed = { it in 'a'..'z' }, isAllowed = NameFlaw::isLetterOrDigitAToZ)) {
        NameFlaw.EMPTY -> NameProblem.NAME_EMPTY
        NameFlaw.LETTER_BEYOND_A_TO_Z -> NameProblem.NAME_LETTER_BEYOND_A_TO_Z
        NameFlaw.FIRST_CHARACTER -> NameProblem.PROPERTY_FIRST_CHARACTER
        NameFlaw.CHARACTER -> NameProblem.PROPERTY_CHARACTERS
        null -> when {
            name in KotlinIdentifiers.hardKeywords -> NameProblem.NAME_KEYWORD
            name in reservedNames || (kind == BlockTypeKind.ACTOR && name in actorMemberNames) || (kind == BlockTypeKind.MANAGER && name in managerMemberNames) || name.startsWith("isTouching") || name.startsWith("textStyle") || TimerNaming.fieldSuffixes.any(name::endsWith) ||
                name.endsWith(BlockScope.MANAGER_FIELD_SUFFIX) -> NameProblem.PROPERTY_RESERVED
            existingNames.any { it.equals(name, ignoreCase = true) } -> NameProblem.PROPERTY_TAKEN
            else -> null
        }
    }
}
