/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ActorCountCodeTest {

    private fun counting(vararg actorTypes: String) = actorTypes.map { BlockDefinition.If(ConditionDefinition.ActorCount(it)) } + BlockDefinition.End
    private val brick = ActorTypeDefinition(name = "Brick")
    private val parent = ActorTypeDefinition(name = "Enemy")
    private val child = ActorTypeDefinition(name = "BigAlien", parent = "Enemy")
    private val actorTypes = listOf(brick, parent, child)
    private val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = counting("Brick", "BigAlien", "Missing", ""))))
    private val families = GeneratedFamilies(actorTypes, listOf(game), emptySet(), ActorTypeFamily(actorTypes))

    private fun actor(definition: ActorTypeDefinition) = ActorTypeCodeGenerator.generateActorType(
        definition,
        "com.example.game",
        knownActorTypeNames = actorTypes.map { it.name },
        managerTypes = listOf(game),
        actorTypes = actorTypes,
        families = families,
    )

    @Test
    fun onlyKnownTypesThatGeneratedConditionsCountAreCounted() {
        val unplayed = ManagerTypeDefinition(name = "Hud", events = listOf(EventDefinition(EventType.EDGE_TOUCHED, blocks = counting("Enemy"))))
        assertEquals(listOf("BigAlien", "Brick"), ActorCountCode.countedTypesOf(listOf(game, unplayed, brick), actorTypes.map { it.name }))
        assertEquals(listOf("BigAlien", "Brick"), families.countedTypes)
    }

    @Test
    fun theConstantsSpellTheTypesInCapitals() {
        assertEquals("ACTORS_BRICK", ActorCountCode.constantOf("Brick"))
        assertEquals("ACTORS_BIG_ALIEN", ActorCountCode.constantOf("BigAlien"))
        assertEquals("ACTORS_LEVEL2_BOSS", ActorCountCode.constantOf("Level2Boss"))
        assertEquals("ACTORS_UFO", ActorCountCode.constantOf("UFO"))
    }

    @Test
    fun theConditionReadsTheCountGeneratedScenesKeeps() {
        val scope = BlockScope("Game", BlockTypeKind.MANAGER, actorTypeNames = listOf("Brick"))
        fun condition(condition: ConditionDefinition) = BlockCodeGenerator.blockCode(BlockDefinition.If(condition), EventType.EVERY_FRAME, scope)
        val code = condition(ConditionDefinition.ActorCount("Brick", Comparison.GREATER_OR_EQUAL, NumberValue.Fixed(2f)))
        assertEquals("if (generatedScenes.actorCount(GeneratedScenes.ACTORS_BRICK).toFloat() >= 2f) {", code.lines.single())
        assertEquals(setOf(BlockNeed.SCENES), code.needs)
        assertEquals("if (false) {", condition(ConditionDefinition.ActorCount("Missing")).lines.single())
        assertEquals("if (false) {", condition(ConditionDefinition.ActorCount()).lines.single())
    }

    @Test
    fun generatedScenesKeepsACountPerCountedType() {
        val code = ScenesCodeGenerator.generate("com.example.game", listOf("BigAlien", "Brick"))
        assertContains(code, "@OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)\nclass GeneratedScenes(")
        assertContains(code, "    private val actorCounts = kotlin.concurrent.atomics.AtomicIntArray(2)\n")
        assertContains(code, "    fun actorCount(type: Int): Int = actorCounts.loadAt(type)\n")
        assertContains(code, "        fun stop() = isCounted.compareAndSet(true, false)\n")
        assertContains(code, "        const val ACTORS_BIG_ALIEN = 0\n        const val ACTORS_BRICK = 1\n    }\n}\n")
        val uncounted = ScenesCodeGenerator.generate("com.example.game")
        assertFalse("atomics" in uncounted)
        assertFalse("OptIn" in uncounted)
        assertContains(uncounted, "        const val BOTTOM = 3\n    }\n}\n")
    }

    @Test
    fun anActorCountsItselfOnceAddedUntilABlockOrKubrikoRemovesIt() {
        val code = actor(brick)
        assertContains(code, "    var isRemoved = false\n        set(value) {\n            field = value\n            if (value) stopCounting()\n        }\n")
        assertContains(code, "    private val actorCountGuard = GeneratedScenes.ActorCountGuard()\n")
        assertContains(
            code,
            "        generatedScenes = kubriko.get()\n" +
                "        if (actorCountGuard.start()) {\n" +
                "            generatedScenes.changeActorCount(GeneratedScenes.ACTORS_BRICK, 1)\n" +
                "        }\n",
        )
        assertContains(code, "    override fun onRemoved() {\n        stopCounting()\n    }\n")
        assertContains(
            code,
            "    private fun stopCounting() {\n" +
                "        if (actorCountGuard.stop()) {\n" +
                "            generatedScenes.changeActorCount(GeneratedScenes.ACTORS_BRICK, -1)\n" +
                "        }\n" +
                "    }\n",
        )
    }

    @Test
    fun aFamilysBaseClassCountsOnlyTheMembersOfTheCountedType() {
        val code = actor(parent)
        assertContains(code, "            if (this is BigAlien) generatedScenes.changeActorCount(GeneratedScenes.ACTORS_BIG_ALIEN, 1)\n")
        assertFalse("ACTORS_ENEMY" in code)
    }

    @Test
    fun anUncountedTypeHasNoneOfIt() {
        val code = ActorTypeCodeGenerator.generateActorType(ActorTypeDefinition(name = "Ball"), "com.example.game", knownActorTypeNames = listOf("Ball", "Brick"), managerTypes = listOf(game))
        assertFalse("actorCountGuard" in code)
        assertFalse("onRemoved" in code)
        assertContains(code, "    var isRemoved = false\n\n")
    }
}
