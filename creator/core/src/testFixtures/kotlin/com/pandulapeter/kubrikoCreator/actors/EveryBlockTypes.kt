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

import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet

/**
 * Block-built types that together use every Event, Block, and condition, with odd values (quotes, dollar signs, missing
 * names) wherever a field takes text: the project the integration test compiles and the generated code golden files pin.
 */
public object EveryBlockTypes {

    private val properties = listOf(
        PropertyDefinition.Number("score", 2.5f),
        PropertyDefinition.Text("title", "Say \"\$hi\" \\ to\neveryone"),
        PropertyDefinition.Toggle("isShielded", true),
    )

    val everyBlock = listOf(
        BlockDefinition.SetVelocity(x = 1.5f, y = -2f),
        BlockDefinition.AddToVelocity(x = 1e-4f, y = 3f),
        BlockDefinition.Stop,
        BlockDefinition.JumpTo(x = -10f, y = 10f),
        BlockDefinition.BounceOffEdge,
        BlockDefinition.RemoveSelf,
        BlockDefinition.BounceOffOther,
        BlockDefinition.RemoveOther,
        BlockDefinition.Log(message = "Say \"\$hi\" \\ to\neveryone"),
        BlockDefinition.Kotlin(code = "val speed = velocity.x\nif (speed > 0f.sceneUnit) velocity = SceneOffset.Zero"),
        BlockDefinition.SetNumber(property = "score", value = -1f),
        BlockDefinition.ChangeNumber(property = "score", by = 0.5f),
        BlockDefinition.SetText(property = "title", value = "Game \"over\""),
        BlockDefinition.SetToggle(property = "isShielded", value = false),
        BlockDefinition.SetNumber(),
        BlockDefinition.ChangeNumber(property = "missing"),
        BlockDefinition.Log(message = "{title}: {score} ({isShielded}) {missing}"),
        BlockDefinition.Kotlin(code = "import kotlin.math.sin\n\nif (isShielded) score += 1f + sin(0f)"),
        BlockDefinition.AddActor("EveryBlock", x = 10f, y = -5f),
        BlockDefinition.AddActor("Ball", isRelative = false),
        BlockDefinition.AddActor("Missing"),
        BlockDefinition.MoveTowardsNearest("Ball", speed = 120f),
        BlockDefinition.MoveTowardsNearest("EveryBlock"),
        BlockDefinition.MoveTowardsNearest(),
        BlockDefinition.StartTimer(timer = "timer1", seconds = 0.25f),
        BlockDefinition.StartTimer(timer = "spawnEnemy", seconds = 2f, isRepeating = true),
        BlockDefinition.StopTimer(timer = "timer2"),
        BlockDefinition.StartTimer(timer = "Not a name"),
        BlockDefinition.End,
        BlockDefinition.If(ConditionDefinition.Number("score", Comparison.LESS_OR_EQUAL, 3f)),
        BlockDefinition.Repeat(times = 2),
        BlockDefinition.If(ConditionDefinition.Text("title", "Say \"\$hi\"", isEqual = false)),
        BlockDefinition.If(ConditionDefinition.Toggle("isShielded")),
        BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice.SPACE, isHeld = false)),
        BlockDefinition.If(ConditionDefinition.Velocity(Axis.VERTICAL, Comparison.NOT_EQUAL, -1.5f)),
        BlockDefinition.If(ConditionDefinition.Chance(12.5f)),
        BlockDefinition.If(ConditionDefinition.Paused(isPaused = false)),
        BlockDefinition.If(ConditionDefinition.Number("missing")),
        BlockDefinition.Stop,
        BlockDefinition.Else,
        BlockDefinition.Else,
        BlockDefinition.RemoveSelf,
        BlockDefinition.End,
        BlockDefinition.ChangeNumber(property = "score", by = 2f, manager = "EveryManager"),
        BlockDefinition.SetText(property = "title", value = "Hi", manager = "EveryManager"),
        BlockDefinition.SetToggle(property = "isShielded", manager = "EveryManager"),
        BlockDefinition.SetNumber(property = "score", manager = "Missing"),
        BlockDefinition.SetNumber(manager = "EveryManager"),
        BlockDefinition.If(ConditionDefinition.Number("score", Comparison.GREATER, 1f, manager = "EveryManager")),
        BlockDefinition.If(ConditionDefinition.Text("title", manager = "EveryManager")),
        BlockDefinition.If(ConditionDefinition.Toggle("isShielded", manager = "EveryManager")),
        BlockDefinition.Log(message = "{EveryManager.score} {EveryManager.title} {EveryManager.isShielded} {Missing.score} {EveryManager.missing}"),
        BlockDefinition.Kotlin(code = "actorManager.allActors.value.size"),
        BlockDefinition.GoToScene("main"),
        BlockDefinition.GoToScene("missing"),
        BlockDefinition.GoToScene(),
        BlockDefinition.RestartScene,
        BlockDefinition.PauseGame(PauseAction.PAUSE),
        BlockDefinition.PauseGame(PauseAction.RESUME),
        BlockDefinition.PauseGame(PauseAction.TOGGLE),
        BlockDefinition.PlaySound("files/sounds/hit.wav"),
        BlockDefinition.PlaySound("files/sounds/say \"\$hi\".wav"),
        BlockDefinition.PlaySound(),
        BlockDefinition.PlayMusic("files/music/theme.mp3"),
        BlockDefinition.PlayMusic("files/music/win.mp3", isLooping = false),
        BlockDefinition.PlayMusic(),
        BlockDefinition.StopMusic,
        BlockDefinition.SetFrame(frame = 2),
        BlockDefinition.SetAnimation(speed = -0.5f, isLooping = false),
    )

    /**
     * Kotlin Blocks are inserted verbatim, so the ones about the Actor's velocity are left out of Manager types.
     */
    val everyManagerBlock = everyBlock.filterNot { it is BlockDefinition.Kotlin && "velocity" in it.code }

    fun everyEvent(blocks: List<BlockDefinition>) = EventType.entries.flatMap { type ->
        when (type) {
            EventType.KEY_PRESSED -> KeyChoice.all.mapIndexed { index, key ->
                // Every key, though only a few with every Block, to keep the project small to compile.
                EventDefinition(
                    type = type,
                    key = key,
                    blocks = if (index < 3) blocks else listOf(BlockDefinition.If(ConditionDefinition.KeyHeld(key)), BlockDefinition.Log("Held"), BlockDefinition.End),
                )
            }
            EventType.COLLISION -> listOf("EveryBlock", "Ball").map { EventDefinition(type = type, otherActorType = it, blocks = blocks + blocks) }
            EventType.TIMER_FINISHED -> listOf("timer1", "spawnEnemy", "timer1").map { EventDefinition(type = type, timer = it, blocks = blocks) }
            else -> listOf(EventDefinition(type = type, blocks = blocks))
        }
    }

    val everyBlockInEveryEvent = ActorTypeDefinition(
        name = "EveryBlock",
        shape = ActorShape.RECTANGLE,
        sprite = "drawable/every-block.png",
        tintsSprite = true,
        properties = properties + PropertyDefinition.Number("every"),
        events = everyEvent(everyBlock),
    )

    /**
     * Three generations of Actor types: every Event in each (so every hook is overridden, running the parent's Event
     * first), a new Property and a changed initial value in the child, collisions with the family by its parent, and
     * a sprite in the grandchild.
     */
    val family = listOf(
        ActorTypeDefinition(
            name = "EveryParent",
            properties = properties,
            events = everyEvent(everyBlock),
        ),
        ActorTypeDefinition(
            name = "EveryChild",
            parent = "EveryParent",
            shape = ActorShape.RECTANGLE,
            properties = listOf(properties.first(), PropertyDefinition.Number("extra", initialValue = 2f)),
            events = everyEvent(listOf(BlockDefinition.RunParentEvent) + everyBlock + BlockDefinition.MoveTowardsNearest("EveryParent", speed = 10f)) +
                EventDefinition(EventType.COLLISION, otherActorType = "EveryParent", blocks = listOf(BlockDefinition.RunParentEvent, BlockDefinition.RemoveOther)),
        ),
        ActorTypeDefinition(
            name = "EveryGrandchild",
            parent = "EveryChild",
            sprite = "drawable/every-block.png",
            properties = listOf(PropertyDefinition.Toggle("isLast")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.RunParentEvent, BlockDefinition.Stop, BlockDefinition.SetToggle("isLast", value = true)))),
        ),
    )

    /**
     * Uses its own Properties both by themselves and by its own name.
     */
    val everyManager = ManagerTypeDefinition(
        name = "EveryManager",
        properties = properties,
        events = everyEvent(everyManagerBlock),
        texts = ScreenPosition.entries.map { ScreenTextDefinition("{score} {EveryManager.title} \"\$\" {missing}", it, size = 12.5f, color = 0x80FF0000) },
    )

    val hud = ManagerTypeDefinition(
        name = "Hud",
        properties = properties,
        events = everyEvent(everyManagerBlock),
        texts = listOf(ScreenTextDefinition("{EveryManager.score} / {score}"), ScreenTextDefinition()),
        kotlinUi = """
            import androidx.compose.foundation.layout.Column
            import androidx.compose.foundation.layout.windowInsetsPadding
            import androidx.compose.foundation.text.BasicText
            import androidx.compose.runtime.collectAsState
            import androidx.compose.ui.Modifier

            Column(modifier = Modifier.windowInsetsPadding(windowInsets)) {
                if (!stateManager.isRunning.collectAsState().value && !isInSceneEditor) BasicText("Paused: ${'$'}title")
                BasicText("${'$'}{everyManagerManager.score + score} ${'$'}{actorManager.allActors.collectAsState().value.size} ${'$'}{viewportManager.size.collectAsState().value}")
            }
        """.trimIndent(),
    )

    /**
     * How the sprite every Actor type above is drawn as is cut into frames, by its name, with an origin across only.
     */
    val spriteSheets: Map<String, SpriteSheet> = mapOf(
        "every-block" to SpriteSheet(frameWidth = 16, frameHeight = 8, frameCount = 3, framesPerRow = 2, framesPerSecond = 12.5f, originX = 4),
    )

    /**
     * Every type above, as a project has them.
     */
    val all: List<BlockTypeDefinition> = listOf(everyBlockInEveryEvent, everyManager, hud) + family
}
