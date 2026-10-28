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

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class ActorFamilyCodeGeneratorTest {
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        properties = listOf(PropertyDefinition.Number("hp", initialValue = 3f)),
        events = listOf(
            EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("hp", by = -1f))),
            EventDefinition(EventType.COLLISION, otherActorType = "Wall", blocks = listOf(BlockDefinition.Stop)),
        ),
    )
    private val bat = ActorTypeDefinition(
        name = "Bat",
        parent = "Enemy",
        shape = ActorShape.RECTANGLE,
        properties = listOf(PropertyDefinition.Number("hp", initialValue = 1f), PropertyDefinition.Toggle("isAsleep")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.RunParentEvent, BlockDefinition.Stop))),
    )
    private val wall = ActorTypeDefinition(name = "Wall", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Enemy")))
    private val actorTypes = listOf(enemy, bat, wall)

    private fun generate(definition: ActorTypeDefinition) = ActorTypeCodeGenerator.generateActorType(
        definition = definition,
        packageName = "com.example.game",
        knownActorTypeNames = actorTypes.map { it.name },
        collidableActorTypeNames = PluginManagers.of(actorTypes).collidableActorTypeNames,
        actorTypes = actorTypes,
    )

    @Test
    fun theTopmostTypeOfAFamilyGetsABaseClassWithItsEventsAsHooks() {
        val code = generate(enemy)
        assertContains(code, "abstract class EnemyBase(\n    final override val body: BoxBody,\n    color: Color,\n    hp: Float,\n    final override val collisionMask: CollisionMask,\n) : Visible, Dynamic, CollisionDetector {")
        assertContains(code, "    override fun update(deltaTimeInMilliseconds: Int) {\n        onEveryFrame()\n")
        assertContains(code, "                is Wall -> onCollisionWithWall(other)\n")
        assertContains(code, "    protected open fun onEveryFrame() {\n        hp += -1f\n    }\n")
        assertContains(code, "class Enemy private constructor(state: State) : EnemyBase(\n    body = state.body,\n    color = state.color,\n    hp = state.hp,\n")
        assertContains(code, "), Editable<Enemy> {")
    }

    @Test
    fun aChildOverridesTheHooksOfItsEventsAndRunsItsParentsWithSuper() {
        val code = generate(bat)
        assertFalse("abstract class" in code)
        assertContains(code, "class Bat private constructor(state: State) : EnemyBase(")
        assertContains(code, "collisionMask = BoxCollisionMask(")
        assertContains(code, "    var isAsleep: Boolean = state.isAsleep\n")
        assertContains(code, "    override fun onEveryFrame() {\n        super.onEveryFrame()\n        velocity = SceneOffset.Zero\n    }\n")
        // The child's own initial value, and the parent's Property kept with the child's.
        assertContains(code, "        @SerialName(\"hp\") val hp: Float = 1f,\n        @SerialName(\"isAsleep\") val isAsleep: Boolean = false,\n")
    }

    @Test
    fun aTypeWithAParentAndChildrenGetsABaseClassOfItsOwn() {
        val vampire = ActorTypeDefinition(name = "Vampire", parent = "Bat", properties = listOf(PropertyDefinition.Text("title")))
        val code = ActorTypeCodeGenerator.generateActorType(bat, "com.example.game", (actorTypes + vampire).map { it.name }, actorTypes = actorTypes + vampire)
        assertContains(code, "abstract class BatBase(\n    body: BoxBody,\n    color: Color,\n    hp: Float,\n    isAsleep: Boolean,\n    collisionMask: CollisionMask,\n) : EnemyBase(\n")
        assertContains(code, "    var isAsleep: Boolean = isAsleep\n")
        assertContains(code, "class Bat private constructor(state: State) : BatBase(")
        val vampireCode = ActorTypeCodeGenerator.generateActorType(vampire, "com.example.game", (actorTypes + vampire).map { it.name }, actorTypes = actorTypes + vampire)
        assertContains(vampireCode, "class Vampire private constructor(state: State) : BatBase(")
        assertContains(vampireCode, "    var title: String = state.title\n")
    }

    @Test
    fun aParentStandsForItsChildrenInOtherTypesCollisions() {
        val code = generate(wall)
        assertContains(code, "listOf(EnemyBase::class)")
        assertContains(code, "is EnemyBase -> {")
    }

    @Test
    fun aTypeWithoutAParentHasNoParentEventToRun() {
        val loner = ActorTypeDefinition(name = "Loner", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.RunParentEvent))))
        assertContains(ActorTypeCodeGenerator.generateActorType(loner, "com.example.game"), "// Only an Actor type with a parent runs its parent's Event.")
    }
}
