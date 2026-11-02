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
import kotlin.test.assertTrue

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
    private val wall = ActorTypeDefinition(name = "Wall", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Enemy", blocks = listOf(BlockDefinition.Stop))))
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
        assertContains(code, "    override fun update(deltaTimeInMilliseconds: Int) {\n        if (isRemoved) return\n        onEveryFrame(deltaTimeInMilliseconds)\n")
        assertContains(code, "                is Wall -> if (!other.isRemoved) onCollisionWithWall(other)\n")
        assertContains(code, "    protected open fun onEveryFrame(deltaTimeInMilliseconds: Int) {\n        hp += -1f\n    }\n")
        assertContains(code, "class Enemy private constructor(state: State) : EnemyBase(\n    body = state.body,\n    color = state.color,\n    hp = state.hp,\n")
        assertContains(code, "), Editable<Enemy> {")
    }

    @Test
    fun aFamilyWhoseOnlyEventIsAddedToTheSceneImportsKubrikoForItsOnAdded() {
        val greeter = ActorTypeDefinition(name = "Greeter", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.Log("Hello")))))
        val loudGreeter = ActorTypeDefinition(name = "LoudGreeter", parent = "Greeter")
        val types = listOf(greeter, loudGreeter)
        val code = ActorTypeCodeGenerator.generateActorType(definition = greeter, packageName = "com.example.game", knownActorTypeNames = types.map { it.name }, actorTypes = types)
        assertContains(code, "    override fun onAdded(kubriko: Kubriko) {\n")
        assertContains(code, "import com.pandulapeter.kubriko.Kubriko\n")
    }

    @Test
    fun aChildOverridesTheHooksOfItsEventsAndRunsItsParentsWithSuper() {
        val code = generate(bat)
        assertFalse("abstract class" in code)
        assertContains(code, "class Bat private constructor(state: State) : EnemyBase(")
        assertContains(code, "collisionMask = BoxCollisionMask(")
        assertContains(code, "    var isAsleep: Boolean = state.isAsleep\n")
        assertContains(code, "    override fun onEveryFrame(deltaTimeInMilliseconds: Int) {\n        super.onEveryFrame(deltaTimeInMilliseconds)\n        velocity = SceneOffset.Zero\n    }\n")
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
        assertContains(code, "is EnemyBase -> if (!other.isRemoved) onCollisionWithEnemy(other)\n")
        assertContains(code, "    private fun onCollisionWithEnemy(other: EnemyBase) {\n")
    }

    @Test
    fun aChildsCollisionEventComesBeforeItsParentsSoTheParentsDoesNotCatchIt() {
        val hunter = ActorTypeDefinition(
            name = "Hunter",
            events = listOf(
                EventDefinition(EventType.COLLISION, otherActorType = "Enemy", blocks = listOf(BlockDefinition.Stop)),
                EventDefinition(EventType.COLLISION, otherActorType = "Bat", blocks = listOf(BlockDefinition.RemoveOther)),
            ),
        )
        val code = ActorTypeCodeGenerator.generateActorType(
            definition = hunter,
            packageName = "com.example.game",
            knownActorTypeNames = (actorTypes + hunter).map { it.name },
            actorTypes = actorTypes + hunter,
        )
        assertTrue(code.indexOf("is Bat -> if (!other.isRemoved) onCollisionWithBat(other)") < code.indexOf("is EnemyBase -> if (!other.isRemoved) onCollisionWithEnemy(other)"), code)
        assertContains(code, "    private fun onCollisionWithBat(other: Bat) {\n        other.isRemoved = true\n        actorManager.remove(other)\n    }\n")
    }

    @Test
    fun theBaseClassOfAFamilyOfShapesUpdatesWhicheverMaskItsActorHas() {
        val code = generate(enemy)
        assertContains(code, "        when (val mask = collisionMask) {\n            is CircleCollisionMask -> mask.radius = body.size.width * body.scale.horizontal / 2\n            is BoxCollisionMask -> mask.rotation = body.rotation\n            else -> Unit\n        }\n")
        assertContains(code, "    override val isAlwaysActive = true\n")
        assertContains(code, "    var isRemoved = false\n")
    }

    @Test
    fun theBaseClassOfAFamilyOfOneShapeKeepsTheMaskAsThatShape() {
        val paddle = ActorTypeDefinition(name = "Paddle", shape = ActorShape.RECTANGLE, events = listOf(EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.Stop))))
        val brick = ActorTypeDefinition(name = "Brick", parent = "Paddle", shape = ActorShape.RECTANGLE)
        val ball = ActorTypeDefinition(name = "Ball", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Paddle")))
        val types = listOf(paddle, brick, ball)
        val code = ActorTypeCodeGenerator.generateActorType(
            definition = paddle,
            packageName = "com.example.game",
            knownActorTypeNames = types.map { it.name },
            collidableActorTypeNames = PluginManagers.of(types).collidableActorTypeNames,
            actorTypes = types,
        )
        assertContains(code, "    final override val collisionMask: BoxCollisionMask,\n")
        assertContains(code, "        collisionMask.rotation = body.rotation\n")
        assertFalse("import com.pandulapeter.kubriko.collision.mask.CollisionMask\n" in code, code)
    }

    @Test
    fun aTypeWithoutAParentHasNoParentEventToRun() {
        val loner = ActorTypeDefinition(name = "Loner", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.RunParentEvent))))
        assertContains(ActorTypeCodeGenerator.generateActorType(loner, "com.example.game"), "// Only an Actor type with a parent runs its parent's Event.")
    }

    @Test
    fun familyTimerFieldsAreDocumentedLikeAStandaloneTypes() {
        val timed = enemy.copy(events = enemy.events + EventDefinition(EventType.TIMER_FINISHED, timer = "timer1"))
        val code = ActorTypeCodeGenerator.generateActorType(timed, "com.example.game", actorTypes.map { it.name }, actorTypes = listOf(timed, bat, wall))
        assertContains(
            code,
            "    /**\n     * Until timer1 finishes, or 0 while it doesn't run, and how often it finishes again, or 0 if it doesn't.\n     */\n" +
                "    protected var timer1RemainingInMilliseconds = 0L\n    protected var timer1PeriodInMilliseconds = 0L\n",
        )
    }

    @Test
    fun aFamilyHandlesKeysInTheOrderOfTheKeyList() {
        val keyed = enemy.copy(events = listOf(EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.ENTER), EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.SPACE)))
        val code = ActorTypeCodeGenerator.generateActorType(keyed, "com.example.game", actorTypes.map { it.name }, actorTypes = listOf(keyed, bat, wall))
        assertTrue(code.indexOf("Key.Spacebar -> ") in 0..<code.indexOf("Key.Enter -> "))
    }

    @Test
    fun aChildOverridingTheAddedHookImportsKubriko() {
        val greeter = ActorTypeDefinition(name = "Greeter", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.Log("Hello")))))
        val loudGreeter = ActorTypeDefinition(name = "LoudGreeter", parent = "Greeter", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.Log("HELLO")))))
        val types = listOf(greeter, loudGreeter)
        val code = ActorTypeCodeGenerator.generateActorType(definition = loudGreeter, packageName = "com.example.game", knownActorTypeNames = types.map { it.name }, actorTypes = types)
        assertContains(code, "    override fun onAddedToScene(kubriko: Kubriko) {\n")
        assertContains(code, "import com.pandulapeter.kubriko.Kubriko\n")
    }

    @Test
    fun aChildsKotlinBlockImportGoesIntoTheChildsFileOnly() {
        val sinBat = bat.copy(events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("import kotlin.math.sin\nhp = sin(hp)")))))
        val types = listOf(enemy, sinBat, wall)
        fun generate(definition: ActorTypeDefinition) = ActorTypeCodeGenerator.generateActorType(definition, "com.example.game", types.map { it.name }, actorTypes = types)
        assertContains(generate(sinBat), "\nimport kotlin.math.sin\n")
        assertFalse("import kotlin.math.sin" in generate(enemy))
    }

    @Test
    fun aChildFileImportsOnlyWhatItsClassesUse() {
        val code = generate(bat)
        assertFalse("import com.pandulapeter.kubriko.collision.CollisionDetector\n" in code)
        assertFalse("import kotlin.reflect.KClass\n" in code)
        assertContains(code, "import com.pandulapeter.kubriko.types.SceneOffset\n")
    }
}
