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

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.AnimationTarget
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorTypeCodeGenerator.SPRITE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.AUDIO_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.SCENES_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.STATE_MANAGER_IMPORT
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.eventCode
import com.pandulapeter.kubrikoCreator.actors.timerName

/**
 * What the generated code of Actors that react to [events] needs: which Events it generates, and which Traits, Timers,
 * and Manager lookups it has. For an Actor type with a parent or children, the events are those of its whole family,
 * since the family's base class does the work for all of them.
 *
 * @param scope The scope of the type, or of a family's topmost type.
 * @param shapes The shapes the Actors are drawn and collide as.
 * @param hasSprite Whether any of them is drawn as a sprite.
 * @param isAnimated Whether any of them is drawn as a sprite cut into frames, which update plays.
 * @param isCollidableAnyway Whether a collision Event of another Actor type involves them.
 * @param containment Which of them stay inside the Scene's bounds.
 * @param isFamily Whether they are a family, whose base class runs the added-to-the-Scene hook even when its Event has
 * no Blocks, since a child may override it.
 * @param eventScopes The Events whose code the generated classes hold, each with the scope it is generated in: for a
 * family, every Actor type's own Events in its own scope, so that the base class declares what any of them needs.
 */
internal class ActorTypeFeatures(
    events: List<EventDefinition>,
    scope: BlockScope,
    val shapes: Set<ActorShape>,
    hasSprite: Boolean,
    val isAnimated: Boolean,
    isCollidableAnyway: Boolean,
    val containment: SceneContainment,
    isFamily: Boolean,
    eventScopes: List<Pair<List<EventDefinition>, BlockScope>> = listOf(events to scope),
) {
    private val knownActorTypeNames = scope.actorTypeNames
    val keyEvents = events.filter { it.type == EventType.KEY_PRESSED && isGenerated(it) }
    val edgeEvents = events.filter { it.type == EventType.EDGE_TOUCHED }
    val leftSceneEvents = events.filter { it.type == EventType.LEFT_SCENE }
    val collisionEvents = events.filter { it.type == EventType.COLLISION && isGenerated(it) }
    val timerEvents = events.filter { it.type == EventType.TIMER_FINISHED && isGenerated(it) }
    val addedEvents = events.filter { it.type == EventType.ADDED_TO_SCENE }
    val everyFrameEvents = events.filter { it.type == EventType.EVERY_FRAME }
    val isCollidable = collisionEvents.isNotEmpty() || isCollidableAnyway

    /**
     * Whether the edge Event runs: a family's base class runs it even without Blocks, since a child may override it.
     */
    val firesEdgeEvent = edgeEvents.any { isFamily || it.blocks.isNotEmpty() }

    /**
     * Whether the leaving Event runs, as [firesEdgeEvent]: never when every Actor stays inside the Scene.
     */
    val firesLeftSceneEvent = containment != SceneContainment.ALL && leftSceneEvents.any { isFamily || it.blocks.isNotEmpty() }

    /**
     * Whether the Actors look at the Scene's bounds after they moved ([SceneBoundsCode]).
     */
    val checksSceneBounds = firesEdgeEvent || firesLeftSceneEvent || containment != SceneContainment.NONE
    val generatedEvents = addedEvents + everyFrameEvents + edgeEvents + leftSceneEvents + keyEvents + collisionEvents + timerEvents
    val timers = (timerEvents.mapNotNull { it.trigger.timer } + generatedEvents.flatMap { event -> event.blocks.mapNotNull { it.timerName } }).filter(TimerNaming::isValid).distinct().sorted()
    private val blockCode = eventScopes.fold(BlockCode.NONE) { code, (events, eventScope) ->
        events.filter(::isGenerated).fold(code) { eventsCode, event -> eventsCode + eventCode(event.blocks, event.type, eventScope) }
    }

    /**
     * What the code of the Events imports, for the class that holds all of it.
     */
    val codeImports = blockCode.imports

    /**
     * What the class declares for the Blocks and the Events: the Scene's bounds, which GeneratedScenes knows, are looked
     * at after every move, and key Events check whether the game is paused.
     */
    private val needs = blockCode.needs + buildSet {
        if (checksSceneBounds) addAll(listOf(BlockNeed.MOVEMENT, BlockNeed.VIEWPORT_MANAGER, BlockNeed.SCENES))
        if (timers.isNotEmpty() || everyFrameEvents.any { it.blocks.isNotEmpty() }) add(BlockNeed.MOVEMENT)
        if (keyEvents.isNotEmpty()) add(BlockNeed.STATE_MANAGER)
        if (isAnimated) add(BlockNeed.MOVEMENT)
    }
    val isDynamic = BlockNeed.MOVEMENT in needs

    /**
     * Whether a box collision mask is built again when the Actor's size or scale changes, since its size is fixed: only
     * a Dynamic Actor's update can change them.
     */
    val rebuildsBoxCollisionMask = isCollidable && isDynamic && ActorShape.RECTANGLE in shapes
    val usesBounceOff = BlockNeed.BOUNCE_OFF in needs
    val usesDirection = BlockNeed.DIRECTION in needs
    val usesNumberText = BlockNeed.NUMBER_TEXT in needs

    /**
     * The values the Animate Blocks animate, each of which the class plays ([AnimateCode]).
     */
    val animatedTargets = AnimationTarget.entries.filter { AnimateCode.needOf(it) in needs }

    /**
     * Whether a Block shows a frame of the sprite, through a function only a type of its own declares when it is used;
     * a family's base class always declares it for its members to override.
     */
    val usesSetFrame = BlockNeed.FRAME in needs
    private val lookupNeeds = BlockNeed.entries.filter { it in needs && lookupOf(it) != null }

    /**
     * The fields the Actors look Managers up in when they are added, as `name: Type`.
     */
    val lookups = lookupNeeds.mapNotNull(::lookupOf) +
        listOfNotNull("$SPRITE_MANAGER_FIELD: SpriteManager".takeIf { hasSprite }) +
        blockCode.managers.sorted().map { "${scope.managerField(it)}: $it" }

    /**
     * What the [lookups] import.
     */
    val lookupImports = lookupNeeds.mapNotNull(::importOf) + listOfNotNull("com.pandulapeter.kubriko.sprites.SpriteManager".takeIf { hasSprite })

    /**
     * Whether the Actors override onAdded, to look the Managers up or to run their added-to-the-Scene Events.
     */
    val overridesOnAdded = lookups.isNotEmpty() || if (isFamily) addedEvents.isNotEmpty() else addedEvents.any { it.blocks.isNotEmpty() }

    /**
     * Whether the code of [event] is generated: Events with what doesn't exist (a key, the other Actor type, or a Timer
     * name) are left out, so that the generated code always compiles.
     */
    private fun isGenerated(event: EventDefinition) = when (event.type) {
        EventType.KEY_PRESSED -> event.trigger.key?.isKnown == true
        EventType.COLLISION -> event.trigger.otherActorType in knownActorTypeNames
        EventType.TIMER_FINISHED -> event.trigger.timer?.let(TimerNaming::isValid) == true
        EventType.ADDED_TO_SCENE, EventType.EVERY_FRAME, EventType.EDGE_TOUCHED, EventType.LEFT_SCENE -> true
        EventType.GAME_STARTED -> false
    }

    private companion object {

        fun lookupOf(need: BlockNeed) = when (need) {
            BlockNeed.ACTOR_MANAGER -> "actorManager: ActorManager"
            BlockNeed.VIEWPORT_MANAGER -> "viewportManager: ViewportManager"
            BlockNeed.KEYBOARD_INPUT_MANAGER -> "keyboardInputManager: KeyboardInputManager"
            BlockNeed.STATE_MANAGER -> "$STATE_MANAGER_FIELD: StateManager"
            BlockNeed.SCENES -> "$SCENES_FIELD: ${ScenesCodeGenerator.SCENES_NAME}"
            BlockNeed.AUDIO -> "$AUDIO_FIELD: ${AudioCodeGenerator.AUDIO_NAME}"
            BlockNeed.MOVEMENT, BlockNeed.NUMBER_TEXT, BlockNeed.BOUNCE_OFF, BlockNeed.DIRECTION, BlockNeed.ANIMATION, BlockNeed.FRAME,
            BlockNeed.SCALE_TWEEN, BlockNeed.OPACITY_TWEEN, BlockNeed.COLOR_TWEEN, BlockNeed.POSITION_TWEEN,
            -> null
        }

        fun importOf(need: BlockNeed) = when (need) {
            BlockNeed.ACTOR_MANAGER -> "com.pandulapeter.kubriko.manager.ActorManager"
            BlockNeed.VIEWPORT_MANAGER -> "com.pandulapeter.kubriko.manager.ViewportManager"
            BlockNeed.KEYBOARD_INPUT_MANAGER -> "com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager"
            BlockNeed.STATE_MANAGER -> STATE_MANAGER_IMPORT
            BlockNeed.SCENES, BlockNeed.AUDIO, BlockNeed.MOVEMENT, BlockNeed.NUMBER_TEXT, BlockNeed.BOUNCE_OFF, BlockNeed.DIRECTION, BlockNeed.ANIMATION, BlockNeed.FRAME,
            BlockNeed.SCALE_TWEEN, BlockNeed.OPACITY_TWEEN, BlockNeed.COLOR_TWEEN, BlockNeed.POSITION_TWEEN,
            -> null
        }
    }
}
