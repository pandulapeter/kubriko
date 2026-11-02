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
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.ABSOLUTE_VALUE_IMPORT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.AUDIO_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.SCENES_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.SCENE_OFFSET_IMPORT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_IMPORT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.eventCode

/**
 * What the generated code of Actors that react to [events] needs: which Events it generates, which Traits, Timers, and
 * Manager lookups it has, and what it imports. For an Actor type with a parent or children, the events are those of
 * its whole family, since the family's base class does the work for all of them.
 *
 * @param shapes The shapes the Actors are drawn and collide as.
 * @param hasSprite Whether any of them is drawn as a sprite.
 * @param isCollidableAnyway Whether a collision Event of another Actor type involves them.
 * @param isFamily Whether they are a family, whose base class runs the added-to-the-Scene hook even when its Event has
 * no Blocks, since a child may override it.
 */
internal class ActorTypeFeatures(
    events: List<EventDefinition>,
    scope: BlockScope,
    packageName: String,
    val shapes: Set<ActorShape>,
    hasSprite: Boolean,
    isCollidableAnyway: Boolean,
    isFamily: Boolean,
) {
    private val knownActorTypeNames = scope.actorTypeNames
    val keyEvents = events.filter { it.type == EventType.KEY_PRESSED && it.key?.isKnown == true }
    val edgeEvents = events.filter { it.type == EventType.EDGE_TOUCHED }
    val collisionEvents = events.filter { it.type == EventType.COLLISION && it.otherActorType in knownActorTypeNames }
    val timerEvents = events.filter { it.type == EventType.TIMER_FINISHED && it.timer?.let(TimerNaming::isValid) == true }
    val addedEvents = events.filter { it.type == EventType.ADDED_TO_SCENE }
    val everyFrameEvents = events.filter { it.type == EventType.EVERY_FRAME }
    val isCollidable = collisionEvents.isNotEmpty() || isCollidableAnyway
    val generatedEvents = addedEvents + everyFrameEvents + edgeEvents + keyEvents + collisionEvents + timerEvents
    val timers = (timerEvents.mapNotNull { it.timer } + generatedEvents.flatMap { event -> event.blocks.mapNotNull { it.timerName } }).filter(TimerNaming::isValid).distinct().sorted()
    private val blockCode = generatedEvents.fold(BlockCode.NONE) { code, event -> code + eventCode(event.blocks, event.type, scope) }

    /**
     * What the class declares for the Blocks and the Events: edge Events move the Actor within the Scene settings' bounds,
     * which GeneratedScenes knows, and key Events check whether the game is paused.
     */
    private val needs = blockCode.needs + buildSet {
        if (edgeEvents.isNotEmpty()) addAll(listOf(BlockNeed.MOVEMENT, BlockNeed.VIEWPORT_MANAGER, BlockNeed.SCENES))
        if (timers.isNotEmpty() || everyFrameEvents.any { it.blocks.isNotEmpty() }) add(BlockNeed.MOVEMENT)
        if (keyEvents.isNotEmpty()) add(BlockNeed.STATE_MANAGER)
    }
    val isDynamic = BlockNeed.MOVEMENT in needs
    val usesBounceOff = BlockNeed.BOUNCE_OFF in needs
    val usesNumberText = BlockNeed.NUMBER_TEXT in needs

    /**
     * The fields the Actors look Managers up in when they are added, as `name: Type`.
     */
    val lookups = BlockNeed.entries.filter { it in needs }.mapNotNull(::lookupOf) +
        listOfNotNull("$SPRITE_MANAGER_FIELD: SpriteManager".takeIf { hasSprite }) +
        blockCode.managers.sorted().map { "${scope.managerField(it)}: $it" }

    /**
     * Whether the Actors override onAdded, to look the Managers up or to run their added-to-the-Scene Events.
     */
    val overridesOnAdded = lookups.isNotEmpty() || if (isFamily) addedEvents.isNotEmpty() else addedEvents.any { it.blocks.isNotEmpty() }

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
        if (edgeEvents.isNotEmpty()) add("com.pandulapeter.kubriko.types.SceneUnit")
        needs.forEach { addAll(importsOf(it)) }
        addAll(blockCode.imports)
        if (overridesOnAdded) add("com.pandulapeter.kubriko.Kubriko")
        if (lookups.isNotEmpty()) add("com.pandulapeter.kubriko.helpers.extensions.get")
        if (hasSprite) addAll(SPRITE_IMPORTS + listOf("$packageName.resources.Res", "$packageName.resources.allDrawableResources"))
    }

    private companion object {

        fun lookupOf(need: BlockNeed) = when (need) {
            BlockNeed.ACTOR_MANAGER -> "actorManager: ActorManager"
            BlockNeed.VIEWPORT_MANAGER -> "viewportManager: ViewportManager"
            BlockNeed.KEYBOARD_INPUT_MANAGER -> "keyboardInputManager: KeyboardInputManager"
            BlockNeed.STATE_MANAGER -> "$STATE_MANAGER_FIELD: StateManager"
            BlockNeed.SCENES -> "$SCENES_FIELD: ${ScenesCodeGenerator.SCENES_NAME}"
            BlockNeed.AUDIO -> "$AUDIO_FIELD: ${AudioCodeGenerator.AUDIO_NAME}"
            BlockNeed.MOVEMENT, BlockNeed.NUMBER_TEXT, BlockNeed.BOUNCE_OFF -> null
        }

        fun importsOf(need: BlockNeed) = when (need) {
            BlockNeed.MOVEMENT -> listOf("com.pandulapeter.kubriko.actor.traits.Dynamic", SCENE_OFFSET_IMPORT)
            BlockNeed.ACTOR_MANAGER -> listOf("com.pandulapeter.kubriko.manager.ActorManager")
            BlockNeed.VIEWPORT_MANAGER -> listOf("com.pandulapeter.kubriko.manager.ViewportManager")
            BlockNeed.KEYBOARD_INPUT_MANAGER -> KEYBOARD_MANAGER_IMPORTS
            BlockNeed.STATE_MANAGER -> listOf(STATE_MANAGER_IMPORT)
            BlockNeed.BOUNCE_OFF -> listOf(ABSOLUTE_VALUE_IMPORT, "com.pandulapeter.kubriko.types.SceneUnit")
            BlockNeed.SCENES, BlockNeed.AUDIO, BlockNeed.NUMBER_TEXT -> emptyList()
        }
    }
}
