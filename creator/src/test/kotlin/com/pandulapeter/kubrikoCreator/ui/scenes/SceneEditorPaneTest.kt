/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.commands.ActorsBroughtBack
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.scenes.MoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActors
import com.pandulapeter.kubrikoCreator.commands.scenes.RestoreActors
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorsValues
import com.pandulapeter.kubrikoCreator.commands.scenes.SetSceneValues
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorTypeLook
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.HealthHints
import com.pandulapeter.kubrikoCreator.ui.LocalHealthHints
import com.pandulapeter.kubrikoCreator.ui.SidebarDragState
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.ui.healthHintsOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SceneEditorPaneTest {
    private val ball = SceneActorEntry("Ball", SceneDocument.blockBuiltState(1f, 2f, 3f, 4f), "ball")
    private val ballBack = ActorsBroughtBack("level", listOf(ball))

    @Test
    fun undoingAnActorsRemovalSelectsItAgain() = uiTest(width = 900, height = 600) {
        val broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = 4)
        val commands = mutableListOf<Invocation<*>>()
        showPane(broughtBack, onCommand = { commands += it })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        broughtBack.tryEmit(ballBack.copy(scene = "other"))
        waitForIdle()
        onNodeWithText(INSPECTOR_HINT).assertExists()
        broughtBack.tryEmit(ballBack)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription(REMOVE).performClick()
        assertEquals(listOf<Invocation<*>>(Invocation(RemoveActor, RemoveActor.Input("level", "ball"))), commands)
        onNodeWithText(INSPECTOR_HINT).assertExists()
        broughtBack.tryEmit(ballBack)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(INSPECTOR_HINT).assertDoesNotExist()
        saveSnapshot("scene-editor-removal-undone")
    }

    @Test
    fun theActorsTheAiChangedLightUp() = uiTest(width = 900, height = 600) {
        var highlight by mutableStateOf<SceneHighlight?>(null)
        showPane(MutableSharedFlow(), actors = { listOf(bigBall) }, previews = mapOf("Ball" to blue), highlight = { highlight })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { ballColor() == Color(blue.color) }
        mainClock.autoAdvance = false
        val before = ballColor()
        highlight = SceneHighlight(key = 1, actorIds = setOf("ball"))
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        saveSnapshot("scene-editor-ai-highlight")
        assertNotEquals(before, ballColor())
        highlight = null
        mainClock.advanceTimeByFrame()
        assertNotEquals(before, ballColor(), "The flash ended at once rather than fading out.")
        mainClock.advanceTimeBy(3_000)
        assertEquals(before, ballColor())
    }

    @Test
    fun actorsThatLitUpBeforeTheSceneWasOpenedAgainDontLightUpAnew() = uiTest(width = 900, height = 600) {
        var highlight by mutableStateOf<SceneHighlight?>(null)
        showPane(MutableSharedFlow(), actors = { listOf(bigBall) }, previews = mapOf("Ball" to blue), highlight = { highlight })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { ballColor() == Color(blue.color) }
        mainClock.autoAdvance = false
        val before = ballColor()
        highlight = SceneHighlight(key = 1, actorIds = setOf("ball"), startedAt = System.currentTimeMillis() - 10_000)
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        assertEquals(before, ballColor())
    }

    private val bigBall = ball.copy(state = SceneDocument.blockBuiltState(0f, 0f, 160f, 100f))
    private val blue = ActorTypeLook(ActorShape.RECTANGLE, color = 0xFF3366CC, width = 120f, height = 80f)

    /**
     * The color at the middle of the Ball, which the Scene's camera centers.
     */
    private fun ComposeUiTest.ballColor(offset: Int = 0) = onRoot().captureToImage().toPixelMap().let { pixels -> pixels[BALL_X + offset, BALL_Y + offset] }

    @Test
    fun theSelectedActorsHandlesResizeAndRotateIt() = uiTest(width = 900, height = 600) {
        val commands = mutableListOf<Invocation<*>>()
        showPane(MutableSharedFlow(), actors = { listOf(bigBall) }, previews = mapOf("Ball" to blue), onCommand = { commands += it })
        waitUntil(timeoutMillis = 5_000) { ballColor() == Color(blue.color) }
        onRoot().performMouseInput { click(Offset(BALL_X.toFloat(), BALL_Y.toFloat())) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
        saveSnapshot("scene-editor-handles")
        onRoot().performMouseInput { dragBy(Offset(BALL_X + 80f, BALL_Y.toFloat()), Offset(40f, 0f)) }
        waitForIdle()
        val resize = (commands.single().input as SetActorsValues.Input).actors.single()
        assertEquals(200f, resize.width!!, absoluteTolerance = 1f)
        assertEquals(100f, resize.height!!, absoluteTolerance = 0.01f)
        assertEquals(20f, resize.x!!, absoluteTolerance = 1f)
        commands.clear()
        // The commands aren't applied, so the Ball stays as the drag left it: 40 pixels wider, its middle 20 to the right.
        onRoot().performMouseInput { dragBy(Offset(BALL_X + 20f, BALL_Y - 50f - 24f), Offset(74f, 74f)) }
        waitForIdle()
        assertEquals(90, (commands.single().input as SetActorsValues.Input).actors.single().rotation?.let(Math::round))
    }

    @Test
    fun movingSnapsToTheGridWhileSnappingIsOnAndAltTurnsItOff() = uiTest(width = 900, height = 600) {
        val commands = mutableListOf<Invocation<*>>()
        showPane(MutableSharedFlow(), actors = { listOf(bigBall) }, previews = mapOf("Ball" to blue), onCommand = { commands += it }, grid = SceneGrid(16f, isSnappingByDefault = true))
        waitUntil(timeoutMillis = 5_000) { ballColor(offset = 5) == Color(blue.color) }
        saveSnapshot("scene-editor-grid")
        onRoot().performMouseInput { dragBy(Offset(BALL_X.toFloat(), BALL_Y.toFloat()), Offset(37f, 5f)) }
        waitForIdle()
        assertEquals(listOf<Invocation<*>>(Invocation(MoveActor, MoveActor.Input("level", "ball", 32f, 0f))), commands)
        commands.clear()
        onRoot().performKeyInput { keyDown(Key.AltLeft) }
        onRoot().performMouseInput { dragBy(Offset(BALL_X + 32f, BALL_Y.toFloat()), Offset(21f, 19f)) }
        onRoot().performKeyInput { keyUp(Key.AltLeft) }
        waitForIdle()
        assertEquals(listOf<Invocation<*>>(Invocation(MoveActor, MoveActor.Input("level", "ball", 53f, 19f))), commands)
    }

    @Test
    fun severalSelectedActorsAreMovedNudgedDuplicatedAndRemovedTogether() = uiTest(width = 900, height = 600) {
        val commands = mutableListOf<Invocation<*>>()
        val wall = ball.copy(id = "wall", state = SceneDocument.blockBuiltState(200f, 0f, 60f, 60f))
        var actors by mutableStateOf(listOf(bigBall, wall))
        showPane(MutableSharedFlow(), actors = { actors }, previews = mapOf("Ball" to blue), onCommand = { commands += it })
        waitUntil(timeoutMillis = 5_000) { ballColor() == Color(blue.color) }
        onRoot().performMouseInput { click(Offset(BALL_X.toFloat(), BALL_Y.toFloat())) }
        onRoot().performKeyInput { keyDown(Key.ShiftLeft) }
        onRoot().performMouseInput { click(Offset(BALL_X + 200f, BALL_Y.toFloat())) }
        onRoot().performKeyInput { keyUp(Key.ShiftLeft) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("2 Actors")).fetchSemanticsNodes().isNotEmpty() }
        saveSnapshot("scene-editor-several-selected")
        onRoot().performMouseInput { dragBy(Offset(BALL_X.toFloat(), BALL_Y.toFloat()), Offset(30f, 20f)) }
        waitForIdle()
        assertEquals(listOf(30f to 20f, 230f to 20f), (commands.single().input as SetActorsValues.Input).actors.map { it.x!! to it.y!! })
        actors = listOf(bigBall.copy(state = SceneDocument.blockBuiltState(30f, 20f, 160f, 100f)), wall.copy(state = SceneDocument.blockBuiltState(230f, 20f, 60f, 60f)))
        waitForIdle()
        commands.clear()
        onRoot().performKeyInput { keyDown(Key.DirectionRight) }
        assertEquals(emptyList(), commands, "A held arrow key reported its move before it was let go.")
        onRoot().performKeyInput { keyUp(Key.DirectionRight) }
        assertEquals(listOf(31f, 231f), (commands.single().input as SetActorsValues.Input).actors.map { it.x!! })
        commands.clear()
        onRoot().performKeyInput { withKeyDown(Key.MetaLeft) { pressKey(Key.D) } }
        val copies = (commands.single().input as RestoreActors.Input).actors
        assertEquals(listOf(2, 3), copies.map { it.actorIndex })
        actors = actors + copies.map { SceneActorEntry(it.typeId, it.state, it.actorId!!) }
        waitForIdle()
        commands.clear()
        onRoot().performKeyInput { pressKey(Key.Delete) }
        assertEquals(copies.map { it.actorId }, (commands.single().input as RemoveActors.Input).actorIds)
    }

    @Test
    fun aHeldArrowKeysMoveIsReportedBeforeAnotherKeyActsAndFollowsTheSceneLoadedAgain() = uiTest(width = 900, height = 600) {
        val commands = mutableListOf<Invocation<*>>()
        var actors by mutableStateOf(listOf(bigBall))
        showPane(MutableSharedFlow(), actors = { actors }, previews = mapOf("Ball" to blue), onCommand = { commands += it })
        waitUntil(timeoutMillis = 5_000) { ballColor() == Color(blue.color) }
        onRoot().performMouseInput { click(Offset(BALL_X.toFloat(), BALL_Y.toFloat())) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
        onRoot().performKeyInput { keyDown(Key.DirectionRight) }
        actors = listOf(ball.copy(state = SceneDocument.blockBuiltState(50f, 7f, 160f, 100f)))
        waitForIdle()
        onRoot().performKeyInput { keyUp(Key.DirectionRight) }
        assertEquals(listOf<Invocation<*>>(Invocation(MoveActor, MoveActor.Input("level", "ball", 51f, 7f))), commands)
        commands.clear()
        onRoot().performKeyInput { keyDown(Key.DirectionRight) }
        onRoot().performKeyInput { pressKey(Key.Delete) }
        onRoot().performKeyInput { keyUp(Key.DirectionRight) }
        assertEquals(listOf(MoveActor, RemoveActor), commands.map { it.command })
    }

    @Test
    fun selectedActorsTheSceneNoLongerHasAreLeftOutOfTheInspectorAndTheRemoval() = uiTest(width = 900, height = 600) {
        val commands = mutableListOf<Invocation<*>>()
        val wall = ball.copy(id = "wall", state = SceneDocument.blockBuiltState(200f, 0f, 60f, 60f))
        var actors by mutableStateOf(listOf(bigBall, wall))
        showPane(MutableSharedFlow(), actors = { actors }, previews = mapOf("Ball" to blue), onCommand = { commands += it })
        waitUntil(timeoutMillis = 5_000) { ballColor() == Color(blue.color) }
        onRoot().performMouseInput { click(Offset(BALL_X.toFloat(), BALL_Y.toFloat())) }
        onRoot().performKeyInput { keyDown(Key.ShiftLeft) }
        onRoot().performMouseInput { click(Offset(BALL_X + 200f, BALL_Y.toFloat())) }
        onRoot().performKeyInput { keyUp(Key.ShiftLeft) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("2 Actors")).fetchSemanticsNodes().isNotEmpty() }
        actors = listOf(wall)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("2 Actors")).fetchSemanticsNodes().isEmpty() }
        onNodeWithContentDescription(REMOVE).assertExists()
        onRoot().performKeyInput { pressKey(Key.Delete) }
        assertEquals(listOf<Invocation<*>>(Invocation(RemoveActor, RemoveActor.Input("level", "wall"))), commands)
    }

    @Test
    fun draggingWithShiftSelectsTheActorsItsBoxTouchesWhereverItStarts() = uiTest(width = 900, height = 600) {
        val wall = ball.copy(id = "wall", state = SceneDocument.blockBuiltState(200f, 0f, 60f, 60f))
        showPane(MutableSharedFlow(), actors = { listOf(bigBall, wall) }, previews = mapOf("Ball" to blue))
        waitUntil(timeoutMillis = 5_000) { ballColor() == Color(blue.color) }
        onRoot().performKeyInput { keyDown(Key.ShiftLeft) }
        onRoot().performMouseInput { dragBy(Offset(BALL_X - 120f, BALL_Y - 80f), Offset(360f, 100f)) }
        onRoot().performKeyInput { keyUp(Key.ShiftLeft) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("2 Actors")).fetchSemanticsNodes().isNotEmpty() }
        onRoot().performKeyInput { pressKey(Key.Escape) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        onRoot().performKeyInput { keyDown(Key.ShiftLeft) }
        onRoot().performMouseInput { dragBy(Offset(BALL_X.toFloat(), BALL_Y.toFloat()), Offset(230f, 10f)) }
        onRoot().performKeyInput { keyUp(Key.ShiftLeft) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("2 Actors")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun MouseInjectionScope.dragBy(start: Offset, by: Offset) {
        moveTo(start)
        press()
        repeat(DRAG_STEPS) { step -> moveTo(start + by * ((step + 1f) / DRAG_STEPS)) }
        release()
    }

    @Test
    fun whatAboutTheSceneIsWorthALookShowsUnderItsHeader() = uiTest(width = 900, height = 600) {
        val unreached = HealthFinding(HealthFinding.Kind.UNREACHED_SCENE, RelationSubject.Scene("level"), "main")
        showPane(emptyFlow(), hints = healthHintsOf(listOf(unreached, HealthFinding(HealthFinding.Kind.UNREACHED_SCENE, RelationSubject.Scene("bonus"), "main")), emptyList()))
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("level: nothing goes to it, and the game starts with main.").assertExists()
        onNodeWithText("bonus", substring = true).assertDoesNotExist()
        saveSnapshot("scene-editor-health-hints")
    }

    @Test
    fun everyActorBroughtBackIntoThisSceneIsSelected() {
        assertEquals(listOf("ball"), restoredSelectionOf(ballBack, "level"))
        assertEquals(emptyList(), restoredSelectionOf(ballBack, "other"))
        assertEquals(listOf("ball", "wall"), restoredSelectionOf(ballBack.copy(actors = listOf(ball, ball.copy(id = "wall"))), "level"))
    }

    @Test
    fun anActorBroughtBackIsSelectedOnlyOnceTheSceneShowsIt() = uiTest(width = 900, height = 600) {
        val broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = 4)
        var actors by mutableStateOf(emptyList<SceneActorEntry>())
        showPane(broughtBack, actors = { actors })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        broughtBack.tryEmit(ballBack)
        waitForIdle()
        onNodeWithText(INSPECTOR_HINT).assertExists()
        actors = listOf(ball)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun anActorTheUserWentToIsSelectedButOneTheAiChangedIsNot() = uiTest(width = 900, height = 600) {
        var highlight by mutableStateOf<SceneHighlight?>(SceneHighlight(key = Any(), actorIds = setOf("ball")))
        showPane(emptyFlow(), highlight = { highlight })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        waitForIdle()
        onNodeWithText(INSPECTOR_HINT).assertExists()
        highlight = SceneHighlight(key = Any(), actorIds = setOf("ball"), isSelecting = true)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun anActorBroughtBackThatTheNextLoadDoesntShowIsDroppedRatherThanSelectedLater() = uiTest(width = 900, height = 600) {
        val broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = 4)
        val wall = ball.copy(id = "wall")
        var actors by mutableStateOf(emptyList<SceneActorEntry>())
        showPane(broughtBack, actors = { actors })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        broughtBack.tryEmit(ballBack)
        waitForIdle()
        actors = listOf(wall)
        waitForIdle()
        actors = listOf(wall, ball)
        waitForIdle()
        onNodeWithText(INSPECTOR_HINT).assertExists()
    }

    @Test
    fun selectingAnActorCancelsARestoredOneButClearingTheSelectionDoesnt() {
        val selection = SceneSelection()
        selection.restore(listOf("ball"))
        selection.select(null)
        assertEquals(listOf("ball"), selection.restoredIds)
        selection.select("wall")
        assertEquals(emptyList(), selection.restoredIds)
        assertEquals("wall", selection.selectedId)
        selection.toggle("ball")
        assertEquals(listOf("wall", "ball"), selection.selectedIds)
        assertNull(selection.selectedId)
        selection.toggle("wall")
        assertEquals("ball", selection.selectedId)
    }

    @Test
    fun theSceneSettingsBroughtBackAreNeverSelected() = uiTest(width = 900, height = 600) {
        val broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = 4)
        val settings = SceneActorEntry(SceneSettingsCodeGenerator.SETTINGS_NAME, "{}", "settings")
        showPane(broughtBack, actors = { listOf(ball, settings) })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        broughtBack.tryEmit(ActorsBroughtBack("level", listOf(settings)))
        waitForIdle()
        onNodeWithText(INSPECTOR_HINT).assertExists()
        onAllNodes(hasContentDescription(REMOVE)).assertCountEquals(0)
    }

    @Test
    fun theValuesTheSceneGivesEachManagerTypeAreShownAndSetBesideTheScene() = uiTest(width = 900, height = 700) {
        val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("lives", initialValue = 3f), PropertyDefinition.Text("title")))
        val commands = mutableListOf<Invocation<*>>()
        showPane(
            emptyFlow(),
            actors = { listOf(ball, SceneActorEntry("Game", """{"lives":5.0}""", "values")) },
            onCommand = { commands += it },
            managerTypes = listOf(game, ManagerTypeDefinition("Empty")),
        )
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(INSPECTOR_HINT)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("title (keeps its value)").assertExists()
        onNodeWithText("Empty").assertDoesNotExist()
        saveSnapshot("scene-editor-manager-values")
        onNodeWithContentDescription("Keep the Manager's value").performClick()
        waitForIdle()
        assertEquals(listOf<Invocation<*>>(Invocation(SetSceneValues, SetSceneValues.Input("level", "Game", cleared = listOf("lives")))), commands)
    }

    @Test
    fun anActorBroughtBackWhileTheSceneIsStillPreparedIsSelectedOnceItShows() = uiTest(width = 900, height = 600) {
        val broughtBack = MutableSharedFlow<ActorsBroughtBack>(extraBufferCapacity = 4)
        val entryPoint = SlowEntryPoint()
        showPane(broughtBack, entryPoint = entryPoint)
        waitUntil(timeoutMillis = 5_000) { broughtBack.subscriptionCount.value > 0 }
        broughtBack.tryEmit(ballBack)
        entryPoint.isReady.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasContentDescription(REMOVE)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.showPane(
        broughtBack: Flow<ActorsBroughtBack>,
        entryPoint: CreatorEntryPoint = PlainEntryPoint,
        actors: () -> List<SceneActorEntry> = { listOf(ball) },
        onCommand: (Invocation<*>) -> Unit = {},
        hints: HealthHints = HealthHints(),
        previews: Map<String, ActorTypeLook> = emptyMap(),
        highlight: () -> SceneHighlight? = { null },
        managerTypes: List<ManagerTypeDefinition> = emptyList(),
        grid: SceneGrid = SceneGrid(size = 16f, isSnappingByDefault = false),
    ) = showing {
        CompositionLocalProvider(LocalHealthHints provides hints, LocalSceneGrid provides grid) { ShownPane(broughtBack, entryPoint, actors, onCommand, previews, highlight(), managerTypes) }
    }

    @Composable
    private fun ShownPane(
        broughtBack: Flow<ActorsBroughtBack>,
        entryPoint: CreatorEntryPoint,
        actors: () -> List<SceneActorEntry>,
        onCommand: (Invocation<*>) -> Unit,
        previews: Map<String, ActorTypeLook>,
        highlight: SceneHighlight?,
        managerTypes: List<ManagerTypeDefinition>,
    ) {
        SceneEditorPane(
            state = SceneEditingState.Open(
                scene = ProjectResource(ResourceGroup.SCENES, File("/game/src/commonMain/composeResources/files/scenes/level.json"), "src/commonMain/composeResources/files/scenes/level.json"),
                entryPoint = entryPoint,
                actors = actors(),
                revision = 1,
                previews = previews,
            ),
            onCommand = onCommand,
            actorTypeDragState = SidebarDragState(),
            panelLayout = PanelLayout(emptyMap(), onSizeChange = { _, _ -> }, onSizeReset = {}),
            actorTypes = emptyList(),
            managerTypes = managerTypes,
            audioFiles = null,
            description = null,
            broughtBack = broughtBack,
            onPlay = {},
            onReload = {},
            onClose = {},
            onViewFailed = { _, _ -> },
            highlight = highlight,
        )
    }

    private class SlowEntryPoint : CreatorEntryPoint {
        val isReady = CompletableDeferred<Unit>()

        override fun createSerializationManager(): SerializationManager<EditableMetadata<*>, Editable<*>> {
            runBlocking { isReady.await() }
            return EditableMetadata.newSerializationManagerInstance()
        }
    }

    private object PlainEntryPoint : CreatorEntryPoint {
        override fun createSerializationManager(): SerializationManager<EditableMetadata<*>, Editable<*>> = EditableMetadata.newSerializationManagerInstance()
    }

    private companion object {
        const val DRAG_STEPS = 10
        const val BALL_X = 339
        const val BALL_Y = 335
        const val INSPECTOR_HINT = "Drag an Actor type from the sidebar into the Scene to place it there, and click an Actor to change it. Drag it to move it, its handles to resize or rotate it (Shift keeps its proportions or turns in steps), or press Delete to remove it. Shift-click or drag with Shift to select several, move them with the arrow keys, and duplicate them with Ctrl or Cmd + D."
        const val REMOVE = "Remove from the Scene"
    }
}
