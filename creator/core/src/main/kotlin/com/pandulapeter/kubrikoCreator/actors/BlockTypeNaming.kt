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
 * The rules for the names of Actor types and Manager types, which are also Kotlin class names (and, for Actor types,
 * Scene type IDs).
 */
public object BlockTypeNaming {

    private val namePattern = Regex("[A-Z][A-Za-z0-9]*")

    /**
     * What the base class of an Actor type with children is named after it (`EnemyBase`), so no type can end with it.
     */
    internal const val BASE_SUFFIX = "Base"
    private val reservedNames = setOf(
        GeneratedNames.ACTOR_TYPES,
        GeneratedNames.WIRING,
        GeneratedNames.SCENES,
        GeneratedNames.SCENE_SETTINGS,
        GeneratedNames.AUDIO,
        GeneratedNames.SPRITES,
        "MusicManager", "SoundManager", "SpriteManager", "Res", "IntSize", "FilterQuality",
        "State", "Color", "Key", "Kubriko", "SceneOffset", "SceneSize", "Manager", "ActorManager", "ViewportManager", "StateManager", "Logger", "Random",
        "Box", "BasicText", "Composable", "Modifier", "Alignment", "TextStyle", "WindowInsets",
        "Actor", "Dynamic", "Unique", "Visible", "Batched", "EllipseLook", "RectangleLook", "SpriteLook", "BoxBody", "DrawScope", "SceneUnit", "Collidable", "CollisionDetector", "CollisionManager",
        "BoxCollisionMask", "CircleCollisionMask", "CollisionMask", "KeyboardInputAware", "KeyboardInputManager", "Editable", "EditableMetadata", "Exposed",
        "Serializable", "SerializationManager", "SerializableBoxBody", "SerializableColor", "SerializableSceneOffset", "SerialName", "Json",
        "KClass", "Volatile", "Job", "CancellationException",
        "Any", "Nothing", "Unit", "Boolean", "Byte", "Short", "Int", "Long", "Float", "Double", "Char", "String", "Array", "List", "Map",
        "Set", "Pair", "Exception",
    )

    /**
     * Manager type names whose lookup field ([BlockScope.managerField]) would be one the generated code has already.
     */
    private val reservedManagerTypeNames = setOf("Viewport", "KeyboardInput", "Sprite")

    /**
     * Whether [name] can be a Kotlin class name the way a type's name has to be.
     */
    internal fun isWellFormed(name: String) = namePattern.matches(name)

    /**
     * Whether [name] is used by Kubriko or by the code Kubriko Creator generates, so a type of that name wouldn't compile.
     */
    internal fun isReserved(name: String, kind: BlockTypeKind) = name in reservedNames || (kind == BlockTypeKind.MANAGER && name in reservedManagerTypeNames)

    /**
     * Why [name] can't be a new Actor type's or Manager type's name, or null if it can. Names that differ only in case
     * are taken too, since they would clash on case-insensitive file systems.
     */
    public fun validate(name: String, existingNames: Collection<String>, kind: BlockTypeKind = BlockTypeKind.ACTOR): NameProblem? = when {
        !isWellFormed(name) -> when (NameFlaw.of(name, isFirstAllowed = { it in 'A'..'Z' }, isAllowed = NameFlaw::isLetterOrDigitAToZ)) {
            NameFlaw.EMPTY -> NameProblem.NAME_EMPTY
            NameFlaw.LETTER_BEYOND_A_TO_Z -> NameProblem.NAME_LETTER_BEYOND_A_TO_Z
            NameFlaw.FIRST_CHARACTER -> NameProblem.TYPE_FIRST_CHARACTER
            NameFlaw.CHARACTER, null -> NameProblem.TYPE_CHARACTERS
        }
        isReserved(name, kind) -> NameProblem.TYPE_RESERVED
        name.endsWith(BASE_SUFFIX) -> NameProblem.TYPE_BASE_SUFFIX
        existingNames.any { it.equals(name, ignoreCase = true) } -> NameProblem.TYPE_TAKEN
        else -> null
    }
}
