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

import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BlockTypeNamingTest {

    @Test
    fun namesAreKotlinClassNames() {
        listOf("Ball", "Enemy2", "PlayerShip").forEach { assertNull(BlockTypeNaming.validate(it, emptyList()), it) }
        listOf("", "ball", "2Ball", "Player Ship", "Player-Ship", "Ballé", "Player_1").forEach {
            assertEquals(NameProblem.TYPE_INVALID, BlockTypeNaming.validate(it, emptyList()), it)
        }
    }

    @Test
    fun namesTheGeneratedCodeAlreadyUsesAreReserved() {
        listOf("GeneratedActorTypes", "KubrikoWiring", "GeneratedScenes", "SceneSettings", "GeneratedAudio", "StateManager", "State", "Color").forEach {
            assertEquals(NameProblem.TYPE_RESERVED, BlockTypeNaming.validate(it, emptyList()), it)
        }
    }

    @Test
    fun managerTypesCantBeNamedAfterALookupTheGeneratedCodeHasAlready() {
        listOf("Viewport", "KeyboardInput", "Sprite").forEach { name ->
            assertEquals(NameProblem.TYPE_RESERVED, BlockTypeNaming.validate(name, emptyList(), BlockTypeKind.MANAGER), name)
            assertNull(BlockTypeNaming.validate(name, emptyList(), BlockTypeKind.ACTOR), name)
        }
    }

    @Test
    fun kotlinsOwnTypesAndEveryClassTheGeneratedCodeImportsAreReserved() {
        val imported = listOf(
            "Color", "DrawScope", "BoxBody", "Visible", "Editable", "EditableMetadata", "Exposed", "Serializable", "SerializableBoxBody",
            "SerializableColor", "SceneSize", "SerialName", "Json", "FilterQuality", "IntSize", "SpriteManager", "Key", "KeyboardInputAware",
            "KeyboardInputManager",
        )
        (imported + listOf("Float", "String", "Unit", "CollisionManager")).forEach {
            assertEquals(NameProblem.TYPE_RESERVED, BlockTypeNaming.validate(it, emptyList()), it)
        }
    }

    @Test
    fun everyClassTheEveryBlockProjectsGeneratedCodeImportsIsReserved() {
        val root = createTempDirectory().toFile()
        try {
            File(root, ".kubriko/project.json").apply { parentFile.mkdirs() }.writeText("""{ "packageName": "com.example.integration" }""")
            File(root, "src/commonMain/composeResources/drawable/every-block.png").apply { parentFile.mkdirs() }.createNewFile()
            val store = assertNotNull(BlockTypeStore.open(root))
            EveryBlockTypes.all.forEach(store::save)
            store.regenerateAll()
            val written = EveryBlockTypes.all.flatMap { type ->
                type.events.flatMap { it.blocks }.filterIsInstance<BlockDefinition.Kotlin>().map { it.code } + listOfNotNull((type as? ManagerTypeDefinition)?.kotlinUi)
            }.flatMapTo(HashSet()) { KotlinUi.splitImports(it).first }
            val imported = File(root, store.generatedPath).walkTopDown().filter { it.isFile }
                .flatMap { file -> file.readLines().filter { it.startsWith("import ") }.map { it.removePrefix("import ") } }
                .filter { it !in written }
                .map { it.substringAfterLast('.') }
                .filter { it.first().isUpperCase() }
                .toSortedSet()
            imported.forEach { assertEquals(NameProblem.TYPE_RESERVED, BlockTypeNaming.validate(it, emptyList()), it) }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun namesMustBeUniqueIgnoringCase() {
        assertEquals(NameProblem.TYPE_TAKEN, BlockTypeNaming.validate("Ball", listOf("Ball")))
        assertEquals(NameProblem.TYPE_TAKEN, BlockTypeNaming.validate("BALL", listOf("Ball")))
        assertNull(BlockTypeNaming.validate("Balls", listOf("Ball")))
    }
}
