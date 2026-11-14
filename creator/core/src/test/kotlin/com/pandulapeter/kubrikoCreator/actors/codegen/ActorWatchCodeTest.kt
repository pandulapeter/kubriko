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
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ActorWatchCodeTest {

    private val log = listOf(BlockDefinition.Log("Gone"))
    private val brick = ActorTypeDefinition(name = "Brick")
    private val parent = ActorTypeDefinition(name = "Enemy")
    private val child = ActorTypeDefinition(name = "Alien", parent = "Enemy")
    private val actorTypes = listOf(brick, parent, child)
    private val game = ManagerTypeDefinition(
        name = "Game",
        properties = listOf(PropertyDefinition.Number("lives")),
        events = listOf(
            EventDefinition(EventType.ACTOR_ADDED, otherActorType = "Brick", blocks = log),
            EventDefinition(EventType.ACTOR_REMOVED, otherActorType = "Brick", blocks = log),
            EventDefinition(EventType.ACTOR_REMOVED, otherActorType = "Alien", blocks = log),
            EventDefinition(EventType.ACTOR_REMOVED, otherActorType = "Enemy"),
            EventDefinition(EventType.ACTOR_ADDED, otherActorType = "Missing", blocks = log),
            EventDefinition(EventType.ACTOR_ADDED),
        ),
    )

    private fun manager(definition: ManagerTypeDefinition = game) = ManagerTypeCodeGenerator.generateManagerType(
        definition,
        "com.example.game",
        actorTypes.map { it.name },
        listOf(definition),
        baseClassNames = setOf("Enemy"),
    )

    private fun actor(definition: ActorTypeDefinition, managers: List<ManagerTypeDefinition> = listOf(game)) = ActorTypeCodeGenerator.generateActorType(
        definition,
        "com.example.game",
        knownActorTypeNames = actorTypes.map { it.name },
        managerTypes = managers,
        actorTypes = actorTypes,
    )

    @Test
    fun aManagerWatchesOnlyTheKnownTypesWhoseEventsHaveBlocks() {
        assertEquals(
            listOf(ActorWatch("Game", "Brick", isAdded = true), ActorWatch("Game", "Alien", isAdded = false), ActorWatch("Game", "Brick", isAdded = false, index = 1)),
            ActorWatch.of(game, actorTypes.map { it.name }),
        )
        assertEquals("notifyBrickRemoved", ActorWatch("Game", "Brick", isAdded = false).notifyFunction)
    }

    @Test
    fun theManagerCountsOnTheAddingThreadAndRunsTheEventsOnItsNextFrame() {
        val code = manager()
        assertContains(code, "@OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)\nclass Game(")
        assertContains(code, "    private val actorsAdded = kotlin.concurrent.atomics.AtomicIntArray(1)\n    private val actorsRemoved = kotlin.concurrent.atomics.AtomicIntArray(2)\n")
        assertContains(
            code,
            "        if (isInSceneEditor || !stateManager.isRunning.value) return\n" +
                "        repeat(actorsAdded.exchangeAt(0, 0)) { onBrickAdded() }\n" +
                "        repeat(actorsRemoved.exchangeAt(0, 0)) { onAlienRemoved() }\n" +
                "        repeat(actorsRemoved.exchangeAt(1, 0)) { onBrickRemoved() }\n",
        )
        assertContains(code, "    fun notifyAlienRemoved() {\n        if (!isInSceneEditor) actorsRemoved.fetchAndAddAt(0, 1)\n    }\n")
        assertContains(code, "    private fun onBrickRemoved() {\n")
        assertFalse("Missing" in code)
        assertFalse("notifyEnemyRemoved" in code)
        assertFalse("import kotlin.concurrent" in code)
    }

    @Test
    fun aManagerWithoutSuchEventsHasNoCounters() {
        val code = manager(ManagerTypeDefinition(name = "Hud", events = listOf(EventDefinition(EventType.ACTOR_ADDED, otherActorType = "Brick"))))
        assertFalse("AtomicIntArray" in code)
        assertFalse("OptIn" in code)
        assertFalse("onUpdate" in code)
    }

    @Test
    fun anActorTellsTheManagerWhenItIsAddedAndRemovedUnlessItLeavesWithItsScene() {
        val code = actor(brick)
        assertContains(code, "    private var sceneChangesWhenAdded = 0\n")
        assertContains(
            code,
            "    override fun onAdded(kubriko: Kubriko) {\n" +
                "        generatedScenes = kubriko.get()\n" +
                "        gameManager = kubriko.get()\n" +
                "        sceneChangesWhenAdded = generatedScenes.sceneChanges\n" +
                "        gameManager.notifyBrickAdded()\n" +
                "    }\n",
        )
        assertContains(code, "    override fun onRemoved() {\n        if (sceneChangesWhenAdded != generatedScenes.sceneChanges) return\n        gameManager.notifyBrickRemoved()\n    }\n")
        assertFalse("onRemoved" in actor(brick, managers = emptyList()))
    }

    @Test
    fun aFamilysBaseClassTellsOnlyAboutTheMembersTheEventIsAbout() {
        val code = actor(parent)
        assertContains(code, "    override fun onRemoved() {\n        if (sceneChangesWhenAdded != generatedScenes.sceneChanges) return\n        if (this is Alien) gameManager.notifyAlienRemoved()\n    }\n")
        assertFalse("notifyEnemy" in code)
    }
}
