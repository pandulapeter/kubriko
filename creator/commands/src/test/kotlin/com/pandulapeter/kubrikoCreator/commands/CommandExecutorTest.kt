/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireType
import com.pandulapeter.kubrikoCreator.commands.project.GetKubrikoApi
import com.pandulapeter.kubrikoCreator.commands.scenes.CreateScene
import com.pandulapeter.kubrikoCreator.commands.scenes.PlaceActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorValues
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneChange
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CommandExecutorTest {
    private val ball = ActorTypeDefinition(name = "Ball", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetVelocity(1f, 1f)))))
    private val rejections = mutableListOf<Rejection>()

    private fun TestScope.executor(vararg definitions: ActorTypeDefinition) = commandContext(*definitions).let { context ->
        CommandExecutor(context = context, scope = testScope(), onRejected = { rejections += it }) to context
    }

    private fun CommandContext.names() = blockTypes!!.actorTypes.value.map { it.name }

    private fun width(width: Float) = Invocation(SetAppearance, SetAppearance.Input("Ball", width = width))

    @Test
    fun sceneChangesAreDeliveredOnce() = runTest {
        val context = commandContext()
        val delivered = mutableListOf<List<SceneChange>>()
        val executor = CommandExecutor(context = context, scope = testScope(), onSceneChanges = { delivered.add(it) })
        executor.execute(Invocation(CreateScene, CreateScene.Input("a")))
        assertEquals<List<List<SceneChange>>>(listOf(listOf(SceneChange.Written("a", isNew = true))), delivered)
        executor.step(isUndo = true)
        assertEquals(listOf(listOf(SceneChange.Written("a", isNew = true)), listOf(SceneChange.Deleted("a"))), delivered)
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Ball")))
        assertEquals(2, delivered.size)
    }

    @Test
    fun changesAreUndoneAndRedoneInOrder() = runTest {
        val (executor, context) = executor()
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Ball")))
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Wall")))
        assertEquals("create_actor_type", executor.history.value.nextUndoCommandName)
        executor.undo()
        assertEquals(listOf("Ball"), context.names())
        executor.undo()
        assertEquals(emptyList(), context.names())
        assertNull(executor.history.value.nextUndoCommandName)
        executor.redo()
        assertEquals(listOf("Ball"), context.names())
        executor.redo()
        assertEquals(listOf("Ball", "Wall"), context.names())
        assertNull(executor.history.value.nextRedoCommandName)
    }

    @Test
    fun theUsersUndoTellsWhichActorsItBroughtBackOnceTheSceneIsWritten() = runTest {
        val context = commandContext()
        val wall = SceneActorEntry("Wall", """{"body":{"position":{"x":1.0,"y":2.0}}}""", "wall")
        @OptIn(EditorMutation::class)
        context.scenes.write("main", SceneDocument.format(listOf(wall)))
        val executor = CommandExecutor(context = context, scope = testScope(), onRejected = { rejections += it })
        val broughtBack = mutableListOf<Pair<ActorsBroughtBack, String?>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { executor.broughtBack.collect { broughtBack += it to context.scenes.read("main") } }
        executor.execute(Invocation(RemoveActor, RemoveActor.Input("main", "wall")))
        executor.step(isUndo = true)
        executor.step(isUndo = false)
        assertEquals(emptyList<Pair<ActorsBroughtBack, String?>>(), broughtBack)
        executor.undo()
        runCurrent()
        assertEquals(listOf<Pair<ActorsBroughtBack, String?>>(ActorsBroughtBack("main", listOf(wall)) to SceneDocument.format(listOf(wall))), broughtBack)
        executor.redo()
        runCurrent()
        assertEquals(1, broughtBack.size)
    }

    @Test
    fun redoingAPlacementTellsWhichActorItBroughtBack() = runTest {
        val context = commandContext(ball)
        @OptIn(EditorMutation::class)
        context.scenes.write("main", "[]")
        val executor = CommandExecutor(context = context, scope = testScope(), onRejected = { rejections += it })
        val broughtBack = mutableListOf<ActorsBroughtBack>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { executor.broughtBack.collect(broughtBack::add) }
        executor.execute(Invocation(PlaceActor, PlaceActor.Input("main", "Ball", 1f, 2f, "ball")))
        executor.undo()
        executor.redo()
        runCurrent()
        assertEquals(listOf("ball"), broughtBack.single().actors.map { it.id })
    }

    @Test
    fun aCompositeChangeTellsWhatItsPartsBroughtBackIntoOneScene() {
        val ballEntry = SceneActorEntry("Ball", "{}", "ball")
        val wallEntry = SceneActorEntry("Wall", "{}", "wall")
        assertEquals(ActorsBroughtBack("main", listOf(ballEntry, wallEntry)), ActorsBroughtBack.merged(listOf(ActorsBroughtBack("main", listOf(ballEntry)), ActorsBroughtBack("main", listOf(wallEntry)))))
        assertNull(ActorsBroughtBack.merged(listOf(ActorsBroughtBack("main", listOf(ballEntry)), ActorsBroughtBack("other", listOf(wallEntry)))))
        assertNull(ActorsBroughtBack.merged(emptyList()))
        val ballAgain = ballEntry.copy(state = "{\"x\":1}")
        assertEquals(ActorsBroughtBack("main", listOf(ballAgain)), ActorsBroughtBack.merged(listOf(ActorsBroughtBack("main", listOf(ballEntry)), ActorsBroughtBack("main", listOf(ballAgain)))))
    }

    @Test
    fun aNewChangeClearsWhatCouldBeRedone() = runTest {
        val (executor, context) = executor(ball)
        executor.execute(width(10f))
        executor.undo()
        assertEquals("set_appearance", executor.history.value.nextRedoCommandName)
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Wall")))
        assertNull(executor.history.value.nextRedoCommandName)
        assertEquals(listOf("Ball", "Wall"), context.names())
    }

    @Test
    fun consecutiveChangesOfTheSameFieldAreUndoneTogether() = runTest {
        val (executor, context) = executor(ball)
        listOf(1f, 12f, 123f).forEach { executor.execute(width(it)) }
        executor.execute(Invocation(SetAppearance, SetAppearance.Input("Ball", height = 5f)))
        listOf(2f, 3f).forEach { executor.execute(Invocation(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.SetVelocity(it, 1f)))) }
        assertEquals(3, executor.history.value.undoable.size)
        assertEquals("edit_block", executor.history.value.nextUndoCommandName)
        executor.undo()
        assertEquals(ball.events, context.blockTypes!!["Ball"]!!.events)
        executor.undo()
        executor.undo()
        assertEquals(ball, context.blockTypes["Ball"])
        executor.redo()
        assertEquals(123f, context.blockTypes.actorType("Ball")!!.width)
    }

    @Test
    fun rejectedChangesChangeNothingAndAreNotRecorded() = runTest {
        val (executor, context) = executor(ball)
        val outcome = executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Ball", width = 5f)))
        assertEquals(CommandOutcome.Rejected(Rejection(NameProblem.TYPE_TAKEN)), outcome)
        assertEquals(listOf(ball), context.blockTypes!!.actorTypes.value)
        assertNull(executor.history.value.nextUndoCommandName)
    }

    @Test
    fun stepsThatNoLongerApplyAreReportedAndDropped() = runTest {
        val (executor, context) = executor(ball)
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Wall")))
        executor.execute(width(10f))
        @OptIn(EditorMutation::class)
        context.blockTypes!!.delete("Ball")
        executor.undo()
        assertEquals(listOf(Rejection(CommandProblem.CHANGED_OUTSIDE, "Ball")), rejections)
        assertEquals("create_actor_type", executor.history.value.nextUndoCommandName)
        assertNull(executor.history.value.nextRedoCommandName)
        executor.undo()
        assertEquals(emptyList(), context.names())
    }

    @Test
    fun toolCallsAreDecodedValidatedAndRecorded() = runTest {
        val (executor, context) = executor(ball)
        val outcome = executor.call("set_appearance", buildJsonObject { put("actorType", "Ball"); put("color", "#112233"); put("shape", "rectangle") })
        assertIs<CommandOutcome.Success>(outcome)
        assertEquals(0xFF112233, context.blockTypes!!.actorType("Ball")!!.color)
        assertEquals("set_appearance", executor.history.value.nextUndoCommandName)
        val event = buildJsonObject { put("actorType", "Ball"); put("eventIndex", 0); put("blockIndex", 0); put("block", buildJsonObject { put("type", "jumpTo"); put("x", 5) }) }
        assertIs<CommandOutcome.Success>(executor.call("edit_block", event))
        assertEquals(BlockDefinition.JumpTo(5f, 0f), context.blockTypes["Ball"]!!.events[0].blocks[0])
    }

    @Test
    fun toolCallsAreRecordedAsStepsThatStartRunningAndNameWhatTheyReadOrChanged() = runTest {
        val (executor, _) = executor(ball)
        val running = mutableListOf<AiStep>()
        val collecting = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { executor.aiSteps.collect { steps -> steps.lastOrNull()?.takeIf { it.isRunning }?.let(running::add) } }
        executor.call("add_block", buildJsonObject { put("actorType", "Ball"); put("eventIndex", 0); put("block", buildJsonObject { put("type", "stop") }) })
        executor.call("read_actor_type", buildJsonObject { put("actorType", "Wall") })
        executor.call("set_appearance", buildJsonObject { put("width", "wide") })
        collecting.cancel()
        assertEquals(
            listOf(
                AiStep(0, "add_block", CommandTarget.BlockType("Ball", 0, 1), isRunning = false, changesProject = true),
                AiStep(1, "read_actor_type", CommandTarget.BlockType("Wall"), isRunning = false, rejection = Rejection(CommandProblem.ACTOR_TYPE_MISSING, "Wall")),
            ),
            executor.aiSteps.value.take(2),
        )
        assertEquals(CommandProblem.INPUT_INVALID, executor.aiSteps.value[2].rejection?.reason)
        assertEquals(listOf(AiStep(0, "add_block", CommandTarget.BlockType("Ball", 0, 0), isRunning = true, changesProject = true)), running.take(1))
    }

    @Test
    fun stepsKeepWhereTheirCallCameFrom() = runTest {
        val (executor, _) = executor(ball)
        val origin = AiStepOrigin(conversationId = "conversation", toolCallId = "toolu_1")
        executor.call("read_actor_type", buildJsonObject { put("actorType", "Ball") }, origin)
        executor.call("set_appearance", buildJsonObject { put("width", "wide") }, origin)
        assertEquals(listOf(origin, origin), executor.aiSteps.value.map { it.origin })
    }

    @Test
    fun aConversationsTurnIsUndoneUpToSomeoneElsesLaterChange() = runTest {
        val (executor, context) = executor(ball)
        val origin = AiStepOrigin(conversationId = "conversation")
        executor.execute(width(10f))
        val turnStart = executor.changeLog.revision
        executor.call("set_appearance", buildJsonObject { put("actorType", "Ball"); put("width", 20) }, origin)
        executor.call("set_appearance", buildJsonObject { put("actorType", "Ball"); put("height", 30) }, origin)
        assertEquals(0, executor.undoTurn("conversation", turnStart))
        val ballNow = context.requireType("Ball") as ActorTypeDefinition
        assertEquals(10f, ballNow.width)
        assertEquals("set_appearance", executor.history.value.nextUndoCommandName)

        executor.call("set_appearance", buildJsonObject { put("actorType", "Ball"); put("width", 40) }, origin)
        executor.execute(width(50f))
        assertEquals(1, executor.undoTurn("conversation", turnStart))
        assertEquals(50f, (context.requireType("Ball") as ActorTypeDefinition).width)
    }

    @Test
    fun aWriteToolSaysTheRevisionAndWhatItChangedWithTheEventAsItIsNow() = runTest {
        val (executor, _) = executor(ball)
        executor.execute(width(10f))
        val result = assertIs<CommandOutcome.Success>(executor.call("add_block", buildJsonObject { put("actorType", "Ball"); put("eventIndex", 0); put("block", buildJsonObject { put("type", "stop") }) })).result
        assertEquals(
            Json.parseToJsonElement("""{"revision":2,"changed":"add_block at Ball#0.1","event":"0 Every frame\n  0 Set velocity to (1, 1)\n  1 Stop"}"""),
            result,
        )
        executor.step(isUndo = true)
        assertEquals(listOf(false, true, false), executor.changeLog.since(0).map { it.isByAi })
        assertEquals(ChangeRecord.Step.UNDO, executor.changeLog.since(0).last().step)
        assertEquals(3, executor.changeLog.revision)
    }

    @OptIn(EditorMutation::class)
    @Test
    fun aWriteToolNamesThePagesMentioningWhatItChanged() = runTest {
        val (executor, context) = executor(ball)
        context.screenplay.write("README", "The [ball](../.kubriko/actors/Ball.json) bounces.")
        context.screenplay.write("other", "Nothing about it.")
        val result = assertIs<CommandOutcome.Success>(executor.call("set_appearance", buildJsonObject { put("actorType", "Ball"); put("width", 40) })).result.jsonObject
        assertEquals(listOf("screenplay/README.md"), result.getValue("mentionedIn").jsonArray.map { it.jsonPrimitive.content })
        context.screenplay.write("README", "No mention any more.")
        val after = assertIs<CommandOutcome.Success>(executor.call("set_appearance", buildJsonObject { put("actorType", "Ball"); put("width", 30) })).result.jsonObject
        assertEquals(null, after["mentionedIn"])
    }

    @OptIn(EditorMutation::class)
    @Test
    fun aDeleteNamesThePagesThatStillMentionWhatItDeleted() = runTest {
        val (executor, context) = executor(ball)
        context.screenplay.write("README", "The [ball](../.kubriko/actors/Ball.json) bounces.")
        val result = assertIs<CommandOutcome.Success>(executor.call("delete_actor_type", buildJsonObject { put("actorType", "Ball") })).result.jsonObject
        assertEquals(listOf("screenplay/README.md"), result.getValue("mentionedIn").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun aToolCallThatThrowsStillFinishesItsStep() = runTest {
        val failing = object : WriteCommand<NoInput> {
            override val name = "fail"
            override val description = "Fails."
            override val inputSerializer = NoInput.serializer()

            override fun validate(context: CommandContext, input: NoInput): Rejection? = null

            override suspend fun apply(context: CommandContext, input: NoInput): Applied = throw NotImplementedError("Broken.")
        }
        val executor = CommandExecutor(context = commandContext(), scope = testScope(), registry = CommandRegistry(listOf(failing)))
        assertFailsWith<NotImplementedError> { executor.call("fail", JsonObject(emptyMap())) }
        val step = executor.aiSteps.value.last()
        assertFalse(step.isRunning)
        assertEquals(CommandProblem.INTERNAL, step.rejection?.reason)
    }

    @Test
    fun anOutsideAiClientsChangeIsNotUndoneWithTheUsers() = runTest {
        val (executor, _) = executor(ball)
        executor.execute(width(10f))
        executor.call("set_appearance", buildJsonObject { put("actorType", "Ball"); put("width", 20) })
        assertEquals(2, executor.history.value.undoable.size)
        executor.execute(width(30f))
        executor.execute(width(40f))
        assertEquals(3, executor.history.value.undoable.size)
    }

    @Test
    fun theUsersOwnChangesAreNotAiSteps() = runTest {
        val (executor, _) = executor(ball)
        executor.execute(width(10f))
        executor.undo()
        assertEquals(emptyList(), executor.aiSteps.value)
    }

    @Test
    fun unknownToolsAndInvalidInputAreRejected() = runTest {
        val (executor, _) = executor(ball)
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.UNKNOWN, "fly")), executor.call("fly", JsonObject(emptyMap())))
        listOf(
            buildJsonObject { put("actorType", "Ball"); put("shape", "triangle") },
            buildJsonObject { put("actorType", "Ball"); put("speed", 3) },
            buildJsonObject { put("width", 3) },
            JsonObject(mapOf("actorType" to JsonPrimitive(3))),
        ).forEach { arguments ->
            val outcome = assertIs<CommandOutcome.Rejected>(executor.call("set_appearance", arguments))
            assertEquals(CommandProblem.INPUT_INVALID, outcome.rejection.reason)
        }
        assertNull(executor.history.value.nextUndoCommandName)
    }

    @Test
    fun changesAreRejectedWhileTheProjectMayNotBeChangedAndReadsStillWork() = runTest {
        val readOnly = Rejection(ProjectProblem.NEWER_FORMAT, "2.0.0")
        var rejection: Rejection? = null
        val context = commandContext(ball, rejectChanges = { rejection })
        val executor = CommandExecutor(context = context, scope = testScope(), onRejected = { rejections += it })
        executor.execute(width(10f))
        rejection = readOnly
        val create = Invocation(CreateActorType, ActorTypeDefinition("Wall"))
        assertEquals(readOnly, executor.validate(create))
        assertEquals(CommandOutcome.Rejected(readOnly), executor.execute(create))
        assertEquals(CommandOutcome.Rejected(readOnly), executor.step(isUndo = true))
        assertIs<CommandOutcome.Success>(executor.call("read_actor_type", buildJsonObject { put("actorType", "Ball") }))
        val check = buildJsonObject { put("tool", "set_appearance"); put("arguments", buildJsonObject { put("actorType", "Ball"); put("width", 20) }) }
        assertEquals(CommandOutcome.Rejected(readOnly), executor.call("check_change", check))
        assertEquals(listOf("Ball"), context.names())
        rejection = null
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
    }

    private fun command(name: String, apply: (NoInput) -> Applied) = object : WriteCommand<NoInput> {
        override val name = name
        override val description = name
        override val inputSerializer = NoInput.serializer()

        override fun validate(context: CommandContext, input: NoInput): Rejection? = null

        override suspend fun apply(context: CommandContext, input: NoInput): Applied = apply(input)
    }

    @Test
    fun aThrowingCommandIsRejected() = runTest {
        val (executor, context) = executor(ball)
        val locked = command("locked") { throw IOException("Locked.") }
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.FAILED, "Locked.")), executor.execute(Invocation(locked, NoInput)))
        assertEquals(emptyList(), executor.history.value.undoable)
        assertIs<CommandOutcome.Success>(executor.execute(width(10f)))
        assertEquals(10f, context.blockTypes!!.actorType("Ball")!!.width)
    }

    @Test
    fun aThrowingUndoStepIsDropped() = runTest {
        val (executor, context) = executor()
        val throwing = command("throwing") { error("Broken.") }
        val revertedByThrowing = command("revertedByThrowing") { Applied(inverse = Invocation(throwing, NoInput)) }
        executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Ball")))
        executor.execute(Invocation(revertedByThrowing, NoInput))
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.FAILED, "Broken.")), executor.step(isUndo = true))
        assertEquals(1, executor.history.value.undoable.size)
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertEquals(emptyList(), context.names())
    }

    @Test
    fun aFailedDeliveryDoesntHideWhatTheChangeOrStepThrew() = runTest {
        val context = commandContext()
        val executor = CommandExecutor(context = context, scope = testScope(), onSceneChanges = { throw IllegalStateException("Not delivered.") })
        val breaking = command("breaking") {
            @OptIn(EditorMutation::class)
            context.scenes.write("level", "[]")
            throw AssertionError("Broken.")
        }
        val revertedByBreaking = command("revertedByBreaking") { Applied(inverse = Invocation(breaking, NoInput)) }
        val changeFailure = assertFailsWith<AssertionError> { executor.execute(Invocation(breaking, NoInput)) }
        assertEquals(listOf("Not delivered."), changeFailure.suppressed.map { it.message })
        executor.execute(Invocation(revertedByBreaking, NoInput))
        val stepFailure = assertFailsWith<AssertionError> { executor.step(isUndo = true) }
        assertEquals(listOf("Not delivered."), stepFailure.suppressed.map { it.message })
    }

    @Test
    fun cancellationIsNotARejection() = runTest {
        val (executor, _) = executor()
        val cancelled = command("cancelled") { throw CancellationException("Stopped.") }
        assertFailsWith<CancellationException> { executor.execute(Invocation(cancelled, NoInput)) }
        assertIs<CommandOutcome.Success>(executor.execute(Invocation(CreateActorType, ActorTypeDefinition("Ball"))))
    }

    private fun TestScope.executorOver(context: CommandContext) = CommandExecutor(context = context, scope = testScope(), onRejected = { rejections += it })

    private val placed = SceneActorEntry("Ball", SceneDocument.blockBuiltState(10f, 20f, 30f, 30f), "i0")

    @OptIn(EditorMutation::class)
    private fun CommandContext.withLevel() = apply { scenes.write("level", SceneDocument.format(listOf(placed))) }

    private fun moveTo(x: Float) = Invocation(SetActorValues, SetActorValues.Input("level", "i0", x = x))

    private fun resizeTo(width: Float) = Invocation(SetActorValues, SetActorValues.Input("level", "i0", width = width))

    private fun editBlock(x: Float) = Invocation(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.SetVelocity(x, 1f)))

    private fun changedOutside(name: String) = CommandOutcome.Rejected(Rejection(CommandProblem.CHANGED_OUTSIDE, name))

    @Test
    fun anOutsideChangeDropsTheStepsOfThatType() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val context = commandContext(blockTypes = blockTypesController(repository = repository))
        val executor = executorOver(context)
        executor.execute(editBlock(2f))
        val reordered = ball.copy(events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Stop, BlockDefinition.SetVelocity(2f, 1f)))))
        repository.stored["Ball"] = reordered
        context.blockTypes!!.reload()
        assertEquals(changedOutside("Ball"), executor.step(isUndo = true))
        assertTrue(executor.history.value.undoable.isEmpty())
        assertEquals(reordered, context.blockTypes["Ball"])
    }

    @Test
    fun anUnchangedReloadKeepsUndoWorking() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val context = commandContext(blockTypes = blockTypesController(repository = repository))
        val executor = executorOver(context)
        executor.execute(editBlock(2f))
        repository.stored["Ball"] = (context.blockTypes!!.actorType("Ball")!!).copy()
        context.blockTypes.reload()
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertEquals(ball, context.blockTypes["Ball"])
    }

    @Test
    fun anOutsideChangeToAnotherTypeKeepsTheStep() = runTest {
        val wall = ActorTypeDefinition("Wall")
        val repository = FakeBlockTypeRepository(listOf(ball, wall))
        val context = commandContext(blockTypes = blockTypesController(repository = repository))
        val executor = executorOver(context)
        executor.execute(editBlock(2f))
        repository.stored["Wall"] = wall.copy(width = 500f)
        context.blockTypes!!.reload()
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertEquals(ball, context.blockTypes["Ball"])
    }

    @Test
    fun aSceneChangedOutsideDropsItsSteps() = runTest {
        val context = commandContext(ball).withLevel()
        val executor = executorOver(context)
        executor.execute(moveTo(50f))
        val file = context.scenes.fileOf("level")
        file.writeText(SceneDocument.format(listOf(placed, placed)))
        file.setLastModified(file.lastModified() + 10_000)
        assertEquals(changedOutside("level"), executor.step(isUndo = true))
        assertTrue(executor.history.value.undoable.isEmpty())
        assertEquals(2, context.scenes.actors("level")!!.size)
    }

    @Test
    fun ownChangesDontInvalidateEarlierSteps() = runTest {
        val context = commandContext(ball).withLevel()
        val executor = executorOver(context)
        listOf(width(10f), moveTo(50f), Invocation(SetAppearance, SetAppearance.Input("Ball", height = 5f)), resizeTo(60f), editBlock(2f)).forEach {
            assertIs<CommandOutcome.Success>(executor.execute(it))
        }
        val changed = context.blockTypes!!["Ball"] to context.scenes.read("level")
        repeat(5) { assertIs<CommandOutcome.Success>(executor.step(isUndo = true)) }
        assertEquals(ball to SceneDocument.format(listOf(placed)), context.blockTypes["Ball"] to context.scenes.read("level"))
        repeat(5) { assertIs<CommandOutcome.Success>(executor.step(isUndo = false)) }
        assertEquals(changed, context.blockTypes["Ball"] to context.scenes.read("level"))
    }

    @Test
    fun aReloadOfCreatorsOwnSaveKeepsUndoWorking() = runTest {
        val root = testDirectory()
        File(root, BlockTypeStore.SETTINGS_FILE).apply { parentFile.mkdirs() }.writeText("""{ "packageName": "com.example.game" }""")
        val context = commandContext(rootDirectory = root, blockTypes = blockTypesController(repository = BlockTypeStore.open(root)!!))
        val executor = executorOver(context)
        val changes = listOf(
            Invocation(CreateActorType, ball),
            editBlock(2f),
            Invocation(SetAppearance, SetAppearance.Input("Ball", width = 12f, color = 0xFF112233)),
            Invocation(AddProperty, AddProperty.Input("Ball", PropertyDefinition.Number("hits", 3f))),
            Invocation(AddEvent, AddEvent.Input("Ball", EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("import kotlin.math.abs\nhits = abs(hits)")))),
        )
        changes.forEach { change ->
            assertIs<CommandOutcome.Success>(executor.execute(change))
            assertTrue(context.blockTypes!!.awaitWrites())
            context.blockTypes.reload()
        }
        repeat(changes.size) { assertIs<CommandOutcome.Success>(executor.step(isUndo = true), "Undo ${it + 1} was rejected: $rejections") }
        assertEquals(emptyList(), context.names())
    }

    @Test
    fun aLockedSceneDoesntDropTheStep() = runTest {
        var failingReads = 0
        val context = commandContext(
            ball,
            sceneReadText = { file ->
                if (failingReads > 0) {
                    failingReads--
                    throw IOException("Locked.")
                }
                file.readText()
            },
        ).withLevel()
        val executor = executorOver(context)
        executor.execute(moveTo(50f))
        val file = context.scenes.fileOf("level")
        file.setLastModified(file.lastModified() + 10_000)
        failingReads = 1
        assertIs<CommandOutcome.Success>(executor.step(isUndo = true))
        assertEquals(0, failingReads)
        assertEquals(SceneDocument.format(listOf(placed)), context.scenes.read("level"))
    }

    private val gate = CompletableDeferred<Unit>()

    /**
     * Changes the Ball type, waits for [gate], then makes the Scene `level` hold an Actor of the type `After`.
     */
    private val gatedChange = object : WriteCommand<NoInput> {
        override val name = "gated"
        override val description = "gated"
        override val inputSerializer = NoInput.serializer()

        override fun validate(context: CommandContext, input: NoInput): Rejection? = null

        @OptIn(EditorMutation::class)
        override suspend fun apply(context: CommandContext, input: NoInput): Applied {
            context.blockTypes!!.put(ball.copy(width = 99f))
            gate.await()
            context.scenes.write("level", SceneDocument.format(listOf(SceneActorEntry("After", "{}", "a"))))
            return Applied(inverse = null)
        }
    }

    @OptIn(EditorMutation::class)
    private fun TestScope.gatedExecutor(registry: CommandRegistry = CommandRegistry.editor) = commandContext(ball).apply {
        scenes.write("level", SceneDocument.format(listOf(SceneActorEntry("Before", "{}", "b"))))
    }.let { context ->
        CommandExecutor(context = context, scope = testScope(), registry = registry).also { executor ->
            launch { executor.execute(Invocation(gatedChange, NoInput)) }
            runCurrent()
        }
    }

    @Test
    fun aReadWaitsForAChangeInProgress() = runTest {
        val executor = gatedExecutor()
        var outcome: CommandOutcome? = null
        launch { outcome = executor.call("read_scene", buildJsonObject { put("scene", "level") }) }
        runCurrent()
        assertNull(outcome)
        gate.complete(Unit)
        advanceUntilIdle()
        val result = assertIs<CommandOutcome.Success>(outcome).result.toString()
        assertTrue("After" in result && "Before" !in result, result)
    }

    @Test
    fun readsDontBlockEachOtherAndActionsDontWaitForAChange() = runTest {
        val (executor, _) = executor(ball)
        assertIs<CommandOutcome.Success>(executor.call("read_actor_type", buildJsonObject { put("actorType", "Ball") }))
        assertIs<CommandOutcome.Success>(executor.call("read_actor_type", buildJsonObject { put("actorType", "Ball") }))
        val held = gatedExecutor()
        var outcome: CommandOutcome? = null
        launch { outcome = held.call("wait", buildJsonObject { put("milliseconds", 10) }) }
        runCurrent()
        assertIs<CommandOutcome.Success>(outcome)
        gate.complete(Unit)
    }

    @Test
    fun aReadOfCreatorsOwnKnowledgeDoesntWaitForAChange() = runTest {
        val ownKnowledge = object : ReadCommand<NoInput> {
            override val name = "own_knowledge"
            override val description = "own_knowledge"
            override val inputSerializer = NoInput.serializer()
            override val readsProject = false

            override suspend fun read(context: CommandContext, input: NoInput) = CommandOutcome.Success(JsonPrimitive("known"))
        }
        val executor = gatedExecutor(CommandRegistry(listOf(ownKnowledge)))
        var outcome: CommandOutcome? = null
        launch { outcome = executor.call("own_knowledge", JsonObject(emptyMap())) }
        runCurrent()
        assertEquals(CommandOutcome.Success(JsonPrimitive("known")), outcome)
        gate.complete(Unit)
        assertFalse(GetKubrikoApi.readsProject)
    }
}
