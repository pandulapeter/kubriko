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
        BlockDefinition.SetVelocity(x = NumberValue.Fixed(1.5f), y = NumberValue.Fixed(-2f)),
        BlockDefinition.AddToVelocity(x = NumberValue.Fixed(1e-4f), y = NumberValue.Fixed(3f)),
        BlockDefinition.Stop,
        BlockDefinition.JumpTo(x = NumberValue.Fixed(-10f), y = NumberValue.Fixed(10f)),
        BlockDefinition.BounceOffEdge,
        BlockDefinition.RemoveSelf,
        BlockDefinition.BounceOffOther,
        BlockDefinition.RemoveOther,
        BlockDefinition.Log(message = "Say \"\$hi\" \\ to\neveryone"),
        BlockDefinition.Kotlin(code = "val speed = velocity.x\nif (speed > 0f.sceneUnit) velocity = SceneOffset.Zero"),
        BlockDefinition.SetNumber(property = "score", value = NumberValue.Fixed(-1f)),
        BlockDefinition.ChangeNumber(property = "score", by = NumberValue.Fixed(0.5f)),
        BlockDefinition.SetText(property = "title", value = "Game \"over\""),
        BlockDefinition.SetToggle(property = "isShielded", value = false),
        BlockDefinition.SetNumber(),
        BlockDefinition.ChangeNumber(property = "missing"),
        BlockDefinition.Log(message = "{title}: {score} ({isShielded}) {missing}"),
        BlockDefinition.Kotlin(code = "import kotlin.math.sin\n\nif (isShielded) score += 1f + sin(0f)"),
        BlockDefinition.AddActor("EveryBlock", x = NumberValue.Fixed(10f), y = NumberValue.Fixed(-5f)),
        BlockDefinition.AddActor("Ball", isRelative = false),
        BlockDefinition.AddActor("Missing"),
        BlockDefinition.MoveTowardsNearest("Ball", speed = NumberValue.Fixed(120f)),
        BlockDefinition.MoveTowardsNearest("EveryBlock"),
        BlockDefinition.MoveTowardsNearest(),
        BlockDefinition.StartTimer(timer = "timer1", seconds = NumberValue.Fixed(0.25f)),
        BlockDefinition.StartTimer(timer = "spawnEnemy", seconds = NumberValue.Fixed(2f), isRepeating = true),
        BlockDefinition.StopTimer(timer = "timer2"),
        BlockDefinition.StartTimer(timer = "Not a name"),
        BlockDefinition.End,
        BlockDefinition.If(ConditionDefinition.Number("score", Comparison.LESS_OR_EQUAL, NumberValue.Fixed(3f))),
        BlockDefinition.Repeat(times = NumberValue.Fixed(2f)),
        BlockDefinition.If(ConditionDefinition.Text("title", "Say \"\$hi\"", isEqual = false)),
        BlockDefinition.If(ConditionDefinition.Toggle("isShielded")),
        BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice.SPACE, isHeld = false)),
        BlockDefinition.If(ConditionDefinition.Velocity(Axis.VERTICAL, Comparison.NOT_EQUAL, NumberValue.Fixed(-1.5f))),
        BlockDefinition.If(ConditionDefinition.Chance(NumberValue.Fixed(12.5f))),
        BlockDefinition.If(ConditionDefinition.Paused(isPaused = false)),
        BlockDefinition.If(ConditionDefinition.Number("missing")),
        BlockDefinition.Stop,
        BlockDefinition.Else,
        BlockDefinition.Else,
        BlockDefinition.RemoveSelf,
        BlockDefinition.End,
        BlockDefinition.ChangeNumber(property = "score", by = NumberValue.Fixed(2f), manager = "EveryManager"),
        BlockDefinition.SetText(property = "title", value = "Hi", manager = "EveryManager"),
        BlockDefinition.SetToggle(property = "isShielded", manager = "EveryManager"),
        BlockDefinition.SetNumber(property = "score", manager = "Missing"),
        BlockDefinition.SetNumber(manager = "EveryManager"),
        BlockDefinition.If(ConditionDefinition.Number("score", Comparison.GREATER, NumberValue.Fixed(1f), manager = "EveryManager")),
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
        BlockDefinition.SetFrame(frame = NumberValue.Fixed(2f)),
        BlockDefinition.SetAnimation(speed = NumberValue.Fixed(-0.5f), isLooping = false),
    ) + numberValueBlocks + edgeValueBlocks + directionBlocks + appearanceBlocks

    /**
     * Blocks taking random numbers and Properties' values, which format version 5 added.
     */
    val numberValueBlocks: List<BlockDefinition>
        get() = listOf(
        BlockDefinition.SetVelocity(x = NumberValue.Random(-50f, 50f), y = NumberValue.Random(3f, 1f, isWhole = true)),
        BlockDefinition.JumpTo(x = NumberValue.Property("score"), y = NumberValue.Property("score", manager = "EveryManager")),
        BlockDefinition.SetNumber(property = "score", value = NumberValue.Random(-1.5f, -1.5f)),
        BlockDefinition.ChangeNumber(property = "score", by = NumberValue.Property("missing")),
        BlockDefinition.StartTimer(timer = "spawnEnemy", seconds = NumberValue.Random(0.5f, 2f)),
        BlockDefinition.StartTimer(timer = "timer1", seconds = NumberValue.Property("score"), isRepeating = true),
        BlockDefinition.Repeat(times = NumberValue.Random(1.5f, 4f)),
        BlockDefinition.SetFrame(frame = NumberValue.Property("score", manager = "EveryManager")),
        BlockDefinition.End,
        BlockDefinition.If(ConditionDefinition.Number("score", Comparison.GREATER, NumberValue.Property("score", manager = "EveryManager"))),
        BlockDefinition.If(ConditionDefinition.Chance(NumberValue.Random(10f, 20f, isWhole = true))),
        BlockDefinition.If(ConditionDefinition.Velocity(Axis.HORIZONTAL, Comparison.LESS, NumberValue.Property(manager = "Missing"))),
        BlockDefinition.End,
        BlockDefinition.End,
        BlockDefinition.End,
        )

    /**
     * Blocks taking where the Scene's edges are, which format version 6 added.
     */
    val edgeValueBlocks: List<BlockDefinition>
        get() = listOf(
            BlockDefinition.AddActor("EveryBlock", x = NumberValue.Edge(SceneEdge.RIGHT, offset = 50f), y = NumberValue.BetweenEdges(SceneEdge.TOP, 10f, SceneEdge.BOTTOM, -10f), isRelative = false),
            BlockDefinition.JumpTo(x = NumberValue.Edge(SceneEdge.LEFT), y = NumberValue.Edge(SceneEdge.BOTTOM, offset = -2.5f)),
            BlockDefinition.ChangeNumber(property = "score", by = NumberValue.Edge(SceneEdge.TOP)),
            BlockDefinition.AddActor("Ball", x = NumberValue.BetweenEdges(SceneEdge.RIGHT, -5f, SceneEdge.LEFT, 5f), y = NumberValue.Edge(SceneEdge.TOP, 1f)),
            BlockDefinition.If(ConditionDefinition.Number("score", Comparison.LESS, NumberValue.BetweenEdges(SceneEdge.TOP, to = SceneEdge.BOTTOM))),
            BlockDefinition.End,
        )

    /**
     * Blocks setting the color and the opacity, which format version 9 added.
     */
    val appearanceBlocks: List<BlockDefinition>
        get() = listOf(
            BlockDefinition.SetColor(),
            BlockDefinition.SetColor(colors = listOf(0xFF20C060, 0xFFFFFFFF, 0xFF000000)),
            BlockDefinition.SetOpacity(percent = NumberValue.Fixed(0f)),
            BlockDefinition.SetOpacity(percent = NumberValue.Random(20f, 80f)),
            BlockDefinition.ChangeOpacity(),
            BlockDefinition.ChangeOpacity(by = NumberValue.Property("score", manager = "EveryManager")),
        )

    /**
     * Blocks moving in a direction, which format version 8 added.
     */
    val directionBlocks: List<BlockDefinition>
        get() = listOf(
            BlockDefinition.MoveInDirection(direction = NumberValue.Fixed(-45f), speed = NumberValue.Fixed(120f)),
            BlockDefinition.MoveInDirection(direction = NumberValue.Random(0f, 360f), speed = NumberValue.Random(100f, 300f)),
            BlockDefinition.MoveInDirection(direction = NumberValue.Property("score"), speed = NumberValue.Property("score", manager = "EveryManager")),
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
        staysInsideScene = true,
        properties = properties + PropertyDefinition.Number("every"),
        events = everyEvent(everyBlock),
    )

    /**
     * Three generations of Actor types: every Event in each (so every hook is overridden, running the parent's Event
     * first), a new Property and a changed initial value in the child, collisions with the family by its parent, a
     * sprite in the grandchild, and only the child staying inside the Scene.
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
            staysInsideScene = true,
            properties = listOf(properties.first(), PropertyDefinition.Number("extra", initialValue = 2f)),
            events = everyEvent(listOf(BlockDefinition.RunParentEvent) + everyBlock + BlockDefinition.MoveTowardsNearest("EveryParent", speed = NumberValue.Fixed(10f))) +
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
