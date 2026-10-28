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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManagerTypeCodeGeneratorTest {
    private val game = ManagerTypeDefinition(
        name = "Game",
        properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Number("lives", initialValue = 3f), PropertyDefinition.Toggle("isOver")),
    )

    private fun generate(definition: ManagerTypeDefinition, actorTypeNames: List<String> = emptyList(), managerTypes: List<ManagerTypeDefinition> = listOf(definition)) =
        ManagerTypeCodeGenerator.generateManagerType(definition, "com.example.game", actorTypeNames, managerTypes)

    @Test
    fun propertiesAreComposeStateThatDoesNotBox() {
        val code = generate(game.copy(properties = game.properties + PropertyDefinition.Text("title", "Hi")))
        assertContains(code, "package com.example.game.generated\n")
        assertContains(code, "class Game(\n    private val isInSceneEditor: Boolean = false,\n) : Manager() {\n")
        assertContains(code, "    var score by mutableFloatStateOf(0f)\n")
        assertContains(code, "    var lives by mutableFloatStateOf(3f)\n")
        assertContains(code, "    var isOver by mutableStateOf(false)\n")
        assertContains(code, "    var title by mutableStateOf(\"Hi\")\n")
        assertContains(code, "import androidx.compose.runtime.getValue\n")
        assertContains(code, "import androidx.compose.runtime.setValue\n")
    }

    @Test
    fun aManagerWithoutEventsHasNoLifecycleOverrides() {
        val code = generate(game)
        assertFalse("onInitialize" in code)
        assertFalse("onUpdate" in code)
        assertFalse("Composable" in code)
    }

    @Test
    fun eventsRunWhenTheGameStartsAndEveryFrameButNotInTheSceneEditor() {
        val code = generate(
            game.copy(
                events = listOf(
                    EventDefinition(EventType.GAME_STARTED, blocks = listOf(BlockDefinition.SetNumber("lives", 3f))),
                    EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.If(ConditionDefinition.Number("lives", Comparison.LESS_OR_EQUAL, 0f)), BlockDefinition.SetToggle("isOver", true))),
                ),
            ),
        )
        assertContains(code, "    override fun onInitialize(kubriko: Kubriko) {\n        if (isInSceneEditor) return\n        // When the game starts\n        lives = 3f\n    }\n")
        assertContains(code, "    override fun onUpdate(deltaTimeInMilliseconds: Int) {\n        if (isInSceneEditor) return\n        if (!stateManager.isRunning.value) return\n        // Every frame\n        if (lives <= 0f) {\n            isOver = true\n        }\n    }\n")
    }

    @Test
    fun keysAndTimersWorkAsInActorTypes() {
        val code = generate(
            game.copy(
                events = listOf(
                    EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.SPACE, blocks = listOf(BlockDefinition.StartTimer(2, 0.5f))),
                    EventDefinition(EventType.TIMER_FINISHED, timer = 2, blocks = listOf(BlockDefinition.ChangeNumber("score", 1f))),
                ),
            ),
        )
        assertContains(code, ") : Manager() {\n")
        assertFalse("KeyboardInputAware" in code)
        assertContains(code, "    private val keyboardInputManager by manager<KeyboardInputManager>()\n")
        assertContains(code, "    private var isKeySpacebarDown = false\n")
        assertContains(
            code,
            "    override fun onUpdate(deltaTimeInMilliseconds: Int) {\n        if (isInSceneEditor) return\n" +
                "        if (keyboardInputManager.isKeyPressed(Key.Spacebar) != isKeySpacebarDown) {\n" +
                "            isKeySpacebarDown = !isKeySpacebarDown\n            if (isKeySpacebarDown) {\n                timer2RemainingInMilliseconds = 500\n",
        )
        assertContains(code, "            }\n        }\n        if (!stateManager.isRunning.value) return\n        if (timer2RemainingInMilliseconds > 0) {\n")
        assertContains(code, "        if (timer2RemainingInMilliseconds > 0) {\n")
        assertContains(code, "                score += 1f\n")
    }

    @Test
    fun blocksThatNeedAnActorBecomeComments() {
        val code = generate(game.copy(events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetVelocity(1f, 1f), BlockDefinition.RemoveSelf, BlockDefinition.If(ConditionDefinition.Velocity()))))))
        assertContains(code, "        // This Block only works in an Actor type.\n        // This Block only works in an Actor type.\n        if (false) {\n")
    }

    @Test
    fun actorsAreAddedRelativeToTheScene() {
        val code = generate(game.copy(events = listOf(EventDefinition(EventType.GAME_STARTED, blocks = listOf(BlockDefinition.AddActor("Ball", 10f, 20f, isRelative = true))))), actorTypeNames = listOf("Ball"))
        assertContains(code, "        actorManager.add(Ball.metadata.instantiate(SceneOffset(10f.sceneUnit, 20f.sceneUnit)).restore())\n")
        assertContains(code, "import com.pandulapeter.kubriko.types.SceneOffset\n")
    }

    @Test
    fun textsAreShownByTheComposableWithTheirProperties() {
        val code = generate(
            game.copy(
                texts = listOf(
                    ScreenTextDefinition("Score: {score}", ScreenPosition.TOP_RIGHT, size = 30f, color = 0xFFFFFFFF),
                    ScreenTextDefinition("Game over", ScreenPosition.CENTER),
                ),
            ),
        )
        assertContains(code, "    @Composable\n    override fun Composable(windowInsets: WindowInsets) {\n")
        assertContains(code, "                modifier = Modifier.align(Alignment.TopEnd),\n                text = \"Score: \${score.asText()}\",\n                style = TextStyle(color = Color(0xFFFFFFFF), fontSize = 30f.sp),\n")
        assertContains(code, "                modifier = Modifier.align(Alignment.Center),\n                text = \"Game over\",\n")
        assertContains(code, "    private fun Float.asText() = ")
        assertContains(code, "import androidx.compose.foundation.text.BasicText\n")
    }

    @Test
    fun otherManagersAreReachedThroughTheirDelegates() {
        val level = ManagerTypeDefinition("Level", properties = listOf(PropertyDefinition.Number("number")))
        val code = generate(
            game.copy(
                events = listOf(EventDefinition(EventType.GAME_STARTED, blocks = listOf(BlockDefinition.ChangeNumber("number", 1f, manager = "Level"), BlockDefinition.SetNumber("score", manager = "Game")))),
                texts = listOf(ScreenTextDefinition("Level {Level.number}")),
            ),
            managerTypes = listOf(game, level),
        )
        assertContains(code, "    private val level by manager<Level>()\n")
        assertContains(code, "        level.number += 1f\n        score = 0f\n")
        assertContains(code, "text = \"Level \${level.number.asText()}\",")
        assertFalse("manager<Game>" in code)
    }

    @Test
    fun theWiringCreatesEveryManagerInNameOrder() {
        val wiring = ManagerTypeCodeGenerator.generateWiring(listOf("Level", "Game"), "com.example.game")
        assertTrue(GeneratedFile.isUnchanged(wiring))
        assertContains(wiring, "object KubrikoWiring {\n")
        assertContains(
            wiring,
            "    fun createManagers(isInSceneEditor: Boolean = false, firstScene: String? = null): List<Manager> = listOf(\n" +
                "        GeneratedScenes(firstScene, isInSceneEditor),\n        Game(isInSceneEditor),\n        Level(isInSceneEditor),\n    )\n",
        )
        assertContains(ManagerTypeCodeGenerator.generateWiring(emptyList(), "com.example.game"), "listOf(\n        GeneratedScenes(firstScene, isInSceneEditor),\n    )\n")
    }

    @Test
    fun sceneAndPauseBlocksUseTheGeneratedScenesAndTheStateManager() {
        val code = generate(
            game.copy(
                events = listOf(
                    EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.ENTER, blocks = listOf(BlockDefinition.GoToScene("level-2"), BlockDefinition.RestartScene, BlockDefinition.GoToScene())),
                    EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.SPACE, blocks = PauseAction.entries.map { BlockDefinition.PauseGame(it) }),
                ),
            ),
        )
        assertContains(code, "import com.pandulapeter.kubriko.manager.StateManager\n")
        assertContains(code, "    private val stateManager by manager<StateManager>()\n    private val generatedScenes by manager<GeneratedScenes>()\n")
        assertContains(code, "                generatedScenes.goTo(\"level-2\")\n                generatedScenes.restart()\n                // Choose a Scene for this Block.\n")
        assertContains(
            code,
            "                stateManager.updateIsRunning(false)\n                stateManager.updateIsRunning(true)\n                stateManager.updateIsRunning(!stateManager.isRunning.value)\n",
        )
    }

    @Test
    fun aManagerWithoutUpdatesOrPauseBlocksDoesNotLookUpTheStateManager() {
        val code = generate(game.copy(events = listOf(EventDefinition(EventType.GAME_STARTED, blocks = listOf(BlockDefinition.GoToScene())))))
        assertFalse("StateManager" in code)
        assertFalse("GeneratedScenes" in code)
    }

    @Test
    fun theHeaderNamesTheDefinition() {
        val code = generate(game)
        assertTrue(code.startsWith("// Generated by Kubriko Creator from .kubriko/managers/Game.json."))
        assertTrue(GeneratedFile.isUnchanged(code))
        assertEquals(1, code.lines().count { it.startsWith("// Generated") })
    }
}
