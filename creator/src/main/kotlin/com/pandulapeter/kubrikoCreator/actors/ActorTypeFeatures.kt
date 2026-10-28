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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.BASE_IMPORTS
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.KEYBOARD_IMPORTS
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.KEYBOARD_MANAGER_IMPORTS
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.SPRITE_IMPORTS
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.SPRITE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.AUDIO_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.SCENES_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_IMPORT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.blockLines

/**
 * What the generated code of Actors that react to [events] needs: which Events it generates, which Traits, Timers, and
 * Manager lookups it has, and what it imports. For an Actor type with a parent or children, the events are those of
 * its whole family, since the family's base class does the work for all of them.
 *
 * @param shapes The shapes the Actors are drawn and collide as.
 * @param hasSprite Whether any of them is drawn as a sprite.
 * @param isCollidableAnyway Whether a collision Event of another Actor type involves them.
 */
internal class ActorTypeFeatures(
    events: List<EventDefinition>,
    scope: BlockScope,
    packageName: String,
    val shapes: Set<ActorShape>,
    hasSprite: Boolean,
    isCollidableAnyway: Boolean,
) {
    private val knownActorTypeNames = scope.actorTypeNames
    val keyEvents = events.filter { it.type == EventType.KEY_PRESSED && it.key != null }
    val edgeEvents = events.filter { it.type == EventType.EDGE_TOUCHED }
    val collisionEvents = events.filter { it.type == EventType.COLLISION && it.otherActorType in knownActorTypeNames }
    val timerEvents = events.filter { it.type == EventType.TIMER_FINISHED && it.timer in TIMER_NUMBERS }
    val addedEvents = events.filter { it.type == EventType.ADDED_TO_SCENE }
    val everyFrameEvents = events.filter { it.type == EventType.EVERY_FRAME }
    val isCollidable = collisionEvents.isNotEmpty() || isCollidableAnyway
    val generatedEvents = addedEvents + everyFrameEvents + edgeEvents + keyEvents + collisionEvents + timerEvents
    val timers = (timerEvents.mapNotNull { it.timer } + generatedEvents.flatMap { event -> event.blocks.mapNotNull { it.timerNumber } }).filter { it in TIMER_NUMBERS }.distinct().sorted()
    val generatedBlocks = generatedEvents.flatMap { it.blocks }
    private val conditions = generatedBlocks.mapNotNull { (it as? BlockDefinition.If)?.condition }
    val usesKeyHeld = conditions.any { it is ConditionDefinition.KeyHeld }
    val usesBounceOff = collisionEvents.any { event -> BlockDefinition.BounceOffOther in event.blocks }

    // Edge Events use the Scene settings' bounds, which GeneratedScenes knows.
    private val usesScenes = BlockCodeGenerator.usesScenes(generatedBlocks) || edgeEvents.isNotEmpty()
    private val usesKotlin = generatedBlocks.any { it is BlockDefinition.Kotlin }
    val isDynamic = usesKotlin || edgeEvents.isNotEmpty() || timers.isNotEmpty() ||
        everyFrameEvents.any { it.blocks.isNotEmpty() } || generatedBlocks.any { it.movesTheActor }
    private val usesActorManager = usesKotlin || generatedBlocks.any { it.needsActorManager(knownActorTypeNames) }
    private val usesViewportManager = usesKotlin || edgeEvents.isNotEmpty()
    private val usesStateManager = BlockCodeGenerator.usesPause(generatedBlocks) || keyEvents.isNotEmpty()

    /**
     * The fields the Actors look Managers up in when they are added, as `name: Type`.
     */
    val lookups = listOfNotNull(
        "actorManager: ActorManager".takeIf { usesActorManager },
        "viewportManager: ViewportManager".takeIf { usesViewportManager },
        "keyboardInputManager: KeyboardInputManager".takeIf { usesKeyHeld },
        "$STATE_MANAGER_FIELD: StateManager".takeIf { usesStateManager },
        "$SCENES_FIELD: ${ScenesCodeGenerator.SCENES_NAME}".takeIf { usesScenes },
        "$AUDIO_FIELD: ${AudioCodeGenerator.AUDIO_NAME}".takeIf { BlockCodeGenerator.usesAudio(generatedBlocks) },
        "$SPRITE_MANAGER_FIELD: SpriteManager".takeIf { hasSprite },
    ) + scope.managersUsedBy(generatedBlocks).map { "${scope.managerField(it)}: $it" }

    val imports = buildSet {
        addAll(BASE_IMPORTS)
        if (isCollidable) {
            add("com.pandulapeter.kubriko.collision.Collidable")
            shapes.forEach { shape ->
                add(
                    when (shape) {
                        ActorShape.CIRCLE -> "com.pandulapeter.kubriko.collision.mask.CircleCollisionMask"
                        ActorShape.RECTANGLE -> "com.pandulapeter.kubriko.collision.mask.BoxCollisionMask"
                    },
                )
            }
        }
        if (collisionEvents.isNotEmpty()) add("com.pandulapeter.kubriko.collision.CollisionDetector")
        if (collisionEvents.isNotEmpty()) add("kotlin.reflect.KClass")
        if (keyEvents.isNotEmpty()) addAll(KEYBOARD_IMPORTS)
        val blocksByEvent = edgeEvents.flatMap { event -> event.blocks.map { it to event.type } } + collisionEvents.flatMap { event -> event.blocks.map { it to event.type } }
        if (usesBounceOff || blocksByEvent.any { (block, eventType) -> blockLines(block, eventType, scope).any { "absoluteValue" in it } }) add("kotlin.math.absoluteValue")
        if (generatedBlocks.any { it is BlockDefinition.Log }) add("com.pandulapeter.kubriko.logger.Logger")
        if (usesBounceOff || edgeEvents.isNotEmpty()) add("com.pandulapeter.kubriko.types.SceneUnit")
        if (usesKeyHeld) addAll(KEYBOARD_MANAGER_IMPORTS)
        if (usesStateManager) add(STATE_MANAGER_IMPORT)
        if (isDynamic) add("com.pandulapeter.kubriko.actor.traits.Dynamic")
        if (usesActorManager) add("com.pandulapeter.kubriko.manager.ActorManager")
        if (usesViewportManager) add("com.pandulapeter.kubriko.manager.ViewportManager")
        if (lookups.isNotEmpty()) addAll(listOf("com.pandulapeter.kubriko.Kubriko", "com.pandulapeter.kubriko.helpers.extensions.get"))
        if (isDynamic || edgeEvents.isNotEmpty() || generatedBlocks.any { it is BlockDefinition.JumpTo || (it is BlockDefinition.AddActor && it.actorType in knownActorTypeNames) }) {
            add("com.pandulapeter.kubriko.types.SceneOffset")
        }
        if (conditions.any { it is ConditionDefinition.Chance }) add("kotlin.random.Random")
        if (hasSprite) addAll(SPRITE_IMPORTS + listOf("$packageName.resources.Res", "$packageName.resources.allDrawableResources"))
        if (generatedBlocks.any { it is BlockDefinition.MoveTowardsNearest && it.actorType in knownActorTypeNames }) {
            add("com.pandulapeter.kubriko.helpers.extensions.normalized")
        }
    }

    private companion object {

        /**
         * Whether [this] Block changes where the Actor is or how it moves, which only an Actor that updates every frame
         * does. A Kotlin Block counts too, since it may.
         */
        val BlockDefinition.movesTheActor
            get() = when (this) {
                is BlockDefinition.SetVelocity, is BlockDefinition.AddToVelocity, BlockDefinition.Stop, is BlockDefinition.JumpTo,
                is BlockDefinition.MoveTowardsNearest, BlockDefinition.BounceOffEdge, BlockDefinition.BounceOffOther, is BlockDefinition.Kotlin -> true
                is BlockDefinition.If -> condition is ConditionDefinition.Velocity
                else -> false
            }

        fun BlockDefinition.needsActorManager(knownActorTypeNames: Collection<String>) = when (this) {
            BlockDefinition.RemoveSelf, BlockDefinition.RemoveOther, is BlockDefinition.Kotlin -> true
            is BlockDefinition.AddActor -> actorType in knownActorTypeNames
            is BlockDefinition.MoveTowardsNearest -> actorType in knownActorTypeNames
            else -> false
        }
    }
}
