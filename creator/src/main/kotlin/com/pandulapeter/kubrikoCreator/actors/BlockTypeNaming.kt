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

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_type_name_base_suffix
import kubriko.creator.generated.resources.error_type_name_invalid
import kubriko.creator.generated.resources.error_type_name_reserved
import kubriko.creator.generated.resources.error_type_name_taken
import org.jetbrains.compose.resources.StringResource

/**
 * The rules for the names of Actor types and Manager types, which are also Kotlin class names (and, for Actor types,
 * Scene type IDs).
 */
internal object BlockTypeNaming {

    private val namePattern = Regex("[A-Z][A-Za-z0-9]*")

    /**
     * What the base class of an Actor type with children is named after it (`EnemyBase`), so no type can end with it.
     */
    const val BASE_SUFFIX = "Base"
    private val reservedNames = setOf(
        ActorTypeCodeGenerator.REGISTRY_NAME,
        ManagerTypeCodeGenerator.WIRING_NAME,
        ScenesCodeGenerator.SCENES_NAME,
        SceneSettingsCodeGenerator.SETTINGS_NAME,
        AudioCodeGenerator.AUDIO_NAME,
        "MusicManager", "SoundManager", "SpriteManager", "Res", "IntSize", "FilterQuality",
        "State", "Color", "Key", "Kubriko", "SceneOffset", "SceneSize", "Manager", "ActorManager", "ViewportManager", "StateManager", "Logger", "Random",
        "Box", "BasicText", "Composable", "Modifier", "Alignment", "TextStyle", "WindowInsets",
        "Actor", "Dynamic", "Unique", "Visible", "BoxBody", "DrawScope", "SceneUnit", "Collidable", "CollisionDetector", "CollisionManager",
        "BoxCollisionMask", "CircleCollisionMask", "KeyboardInputAware", "KeyboardInputManager", "Editable", "EditableMetadata", "Exposed",
        "Serializable", "SerializationManager", "SerializableBoxBody", "SerializableColor", "SerializableSceneOffset", "SerialName", "Json",
        "KClass", "Volatile", "Job", "CancellationException",
        "Any", "Nothing", "Unit", "Boolean", "Byte", "Short", "Int", "Long", "Float", "Double", "Char", "String", "Array", "List", "Map",
        "Set", "Pair", "Exception",
    )

    /**
     * Why [name] can't be a new Actor type's or Manager type's name, or null if it can. Names that differ only in case
     * are taken too, since they would clash on case-insensitive file systems.
     */
    fun validate(name: String, existingNames: Collection<String>): StringResource? = when {
        !namePattern.matches(name) -> Res.string.error_type_name_invalid
        name in reservedNames -> Res.string.error_type_name_reserved
        name.endsWith(BASE_SUFFIX) -> Res.string.error_type_name_base_suffix
        existingNames.any { it.equals(name, ignoreCase = true) } -> Res.string.error_type_name_taken
        else -> null
    }
}
