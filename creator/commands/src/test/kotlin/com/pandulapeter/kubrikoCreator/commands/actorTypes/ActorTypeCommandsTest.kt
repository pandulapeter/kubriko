/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.PauseAction
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.blockTypesController
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ActorTypeCommandsTest {
    private val stop = BlockDefinition.Stop
    private val jump = BlockDefinition.JumpTo(1f, 2f)
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(stop, jump)),
            EventDefinition(EventType.KEY_PRESSED, KeyChoice.LEFT),
            EventDefinition(EventType.COLLISION, otherActorType = "Wall", blocks = listOf(BlockDefinition.RemoveOther)),
        ),
    )
    private val wall = ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE)

    private fun CommandContext.ball() = blockTypes!!["Ball"]

    private fun CommandContext.all() = blockTypes!!.actorTypes.value

    private fun <I> CommandContext.rejection(command: WriteCommand<I>, input: I) = command.validate(this, input)

    @Test
    fun actorTypesAreCreatedRenamedAndDeleted() = runTest {
        val context = commandContext(wall)
        assertEquals(listOf(ball, wall), context.assertRoundTrip(Invocation(CreateActorType, ball)) { context.all() })
        val renamed = context.assertRoundTrip(Invocation(RenameActorType, RenameActorType.Input("Wall", "Brick"))) { context.all() }
        assertEquals(listOf("Ball", "Brick"), renamed.map { it.name })
        assertEquals("Brick", renamed.first().events[2].otherActorType)
        assertEquals(listOf("Ball"), context.assertRoundTrip(Invocation(DeleteActorType, DeleteActorType.Input("Brick"))) { context.all() }.map { it.name })
    }

    @Test
    fun deletingAnActorTypeIsUndoneWithEverythingItHad() = runTest {
        val context = commandContext(ball, wall)
        val inverse = DeleteActorType.apply(context, DeleteActorType.Input("Ball")).inverse!!
        inverse.apply(context)
        assertEquals(ball, context.ball())
    }

    @Test
    fun namesAndContentsOfNewActorTypesAreValidated() = runTest {
        val context = commandContext(ball, wall)
        assertEquals(Rejection(NameProblem.TYPE_FIRST_CHARACTER), context.rejection(CreateActorType, ActorTypeDefinition("ball game")))
        assertEquals(Rejection(NameProblem.TYPE_TAKEN), context.rejection(CreateActorType, ActorTypeDefinition("WALL")))
        assertEquals(Rejection(CommandProblem.SIZE_INVALID), context.rejection(CreateActorType, ActorTypeDefinition("Paddle", width = 0f)))
        assertEquals(Rejection(PlacementProblem.KEY_NOT_ALLOWED), context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, key = KeyChoice.A)))))
        assertEquals(
            Rejection(PlacementProblem.OTHER_ACTOR_TYPE_NOT_ALLOWED),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, otherActorType = "Ball")))),
        )
        assertEquals(
            Rejection(LookupProblem.ACTOR_TYPE_MISSING, "Ghost"),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Ghost")))),
        )
        assertNull(context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Paddle")))))
        assertEquals(
            Rejection(PlacementProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.BounceOffEdge))))),
        )
        assertEquals(
            Rejection(CommandProblem.NUMBER_INVALID),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetVelocity(Float.NaN, 0f)))))),
        )
        assertEquals(Rejection(NameProblem.TYPE_TAKEN), context.rejection(RenameActorType, RenameActorType.Input("Ball", "Wall")))
        assertEquals(Rejection(LookupProblem.ACTOR_TYPE_MISSING, "Ghost"), context.rejection(DeleteActorType, DeleteActorType.Input("Ghost")))
    }

    @Test
    fun newActorTypesAreValidatedAgainstTheProject() = runTest {
        val context = commandContext(ball.copy(properties = listOf(PropertyDefinition.Number("hp"))), wall, spriteFiles = SpriteFiles())
        val setsHp = EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetNumber("hp", 1f)))
        assertNull(context.rejection(CreateActorType, ActorTypeDefinition("Bat", parent = "Ball", events = listOf(setsHp))))
        assertEquals(Rejection(LookupProblem.ACTOR_TYPE_MISSING, "Nope"), context.rejection(CreateActorType, ActorTypeDefinition("Bat", parent = "Nope")))
        assertEquals(
            Rejection(LookupProblem.SCENE_MISSING, "nowhere"),
            context.rejection(CreateActorType, ActorTypeDefinition("Bat", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.GoToScene("nowhere")))))),
        )
        assertEquals(Rejection(LookupProblem.SPRITE_MISSING, "drawable/bat.png"), context.rejection(CreateActorType, ActorTypeDefinition("Bat", sprite = "drawable/bat.png")))
    }

    @Test
    fun projectsWithoutBlockBuiltActorTypesRejectActorTypeCommands() = runTest {
        val context = commandContext(blockTypes = null)
        assertEquals(Rejection(CommandProblem.NO_ACTOR_TYPES), context.rejection(CreateActorType, ball))
        assertEquals(Rejection(CommandProblem.NO_ACTOR_TYPES), context.rejection(SetAppearance, SetAppearance.Input("Ball", width = 1f)))
    }

    @Test
    fun appearanceChangesOnlyWhatIsGivenAndIsUndoneTheSameWay() = runTest {
        val context = commandContext(ball)
        val changed = context.assertRoundTrip(Invocation(SetAppearance, SetAppearance.Input("Ball", color = 0xFF112233, height = 12f))) { context.ball() }
        assertEquals(ball.copy(color = 0xFF112233, height = 12f), changed)
        assertEquals(Rejection(CommandProblem.SIZE_INVALID), context.rejection(SetAppearance, SetAppearance.Input("Ball", width = -1f)))
        assertEquals(SetAppearance.mergeKey(SetAppearance.Input("Ball", width = 1f)), SetAppearance.mergeKey(SetAppearance.Input("Ball", width = 2f)))
        assertEquals(false, SetAppearance.mergeKey(SetAppearance.Input("Ball", width = 1f)) == SetAppearance.mergeKey(SetAppearance.Input("Ball", height = 1f)))
    }

    @Test
    fun choosingASpriteSizesTheActorTypeToTheImageUnlessASizeIsGiven() = runTest {
        val root = createTempDirectory().toFile()
        val image = File(root, "ball.png").apply { ImageIO.write(BufferedImage(24, 16, BufferedImage.TYPE_INT_ARGB), "png", this) }
        val context = commandContext(ball, rootDirectory = root, spriteFiles = SpriteFiles(mapOf("drawable/ball.png" to image)))
        val withSprite = context.assertRoundTrip(Invocation(SetAppearance, SetAppearance.Input("Ball", sprite = "drawable/ball.png"))) { context.ball() }
        assertEquals(ball.copy(sprite = "drawable/ball.png", width = 24f, height = 16f), withSprite)
        val resized = context.assertRoundTrip(Invocation(SetAppearance, SetAppearance.Input("Ball", sprite = "drawable/ball.png", width = 50f))) { context.ball() }
        assertEquals(ball.copy(sprite = "drawable/ball.png", width = 50f, height = 16f), resized)
        assertEquals(ball.copy(width = 50f, height = 16f), context.assertRoundTrip(Invocation(SetAppearance, SetAppearance.Input("Ball", sprite = ""))) { context.ball() })
        assertEquals(Rejection(LookupProblem.SPRITE_MISSING, "drawable/ghost.png"), context.rejection(SetAppearance, SetAppearance.Input("Ball", sprite = "drawable/ghost.png")))
        assertEquals(Rejection(LookupProblem.SPRITE_MISSING, "files/ball.png"), commandContext(ball).rejection(SetAppearance, SetAppearance.Input("Ball", sprite = "files/ball.png")))
        assertNull(commandContext(ball).rejection(SetAppearance, SetAppearance.Input("Ball", sprite = "drawable/unknown.png")))
        root.deleteRecursively()
    }

    @Test
    fun eventsAreAddedMovedRekeyedAndRemoved() = runTest {
        val context = commandContext(ball, wall)
        val added = context.assertRoundTrip(Invocation(AddEvent, AddEvent.Input("Ball", EventType.KEY_PRESSED, index = 1))) { context.ball() }
        assertEquals(EventDefinition(EventType.KEY_PRESSED, KeyChoice.SPACE), added!!.events[1])
        val moved = context.assertRoundTrip(Invocation(MoveEvent, MoveEvent.Input("Ball", eventIndex = 0, targetIndex = 3))) { context.ball() }
        assertEquals(listOf(EventType.KEY_PRESSED, EventType.KEY_PRESSED, EventType.COLLISION, EventType.ADDED_TO_SCENE), moved!!.events.map { it.type })
        assertEquals(KeyChoice.D, context.assertRoundTrip(Invocation(SetEventKey, SetEventKey.Input("Ball", 0, KeyChoice.D))) { context.ball() }!!.events[0].key)
        assertEquals(null, context.assertRoundTrip(Invocation(SetEventOtherActorType, SetEventOtherActorType.Input("Ball", 2, null))) { context.ball() }!!.events[2].otherActorType)
        assertEquals(3, context.assertRoundTrip(Invocation(RemoveEvent, RemoveEvent.Input("Ball", 3))) { context.ball() }!!.events.size)
    }

    @Test
    fun removingAnEventIsUndoneWithItsBlocksInItsPlace() = runTest {
        val context = commandContext(ball, wall)
        RemoveEvent.apply(context, RemoveEvent.Input("Ball", 0)).inverse!!.apply(context)
        assertEquals(ball, context.ball())
    }

    @Test
    fun eventChangesAreValidated() = runTest {
        val context = commandContext(ball, wall)
        assertEquals(Rejection(LookupProblem.EVENT_MISSING, "Ball", "3", "2"), context.rejection(RemoveEvent, RemoveEvent.Input("Ball", 3)))
        assertEquals(Rejection(LookupProblem.INDEX_INVALID, "3", "2"), context.rejection(MoveEvent, MoveEvent.Input("Ball", 0, 3)))
        assertEquals(Rejection(PlacementProblem.KEY_NOT_ALLOWED), context.rejection(SetEventKey, SetEventKey.Input("Ball", 0, KeyChoice.A)))
        assertEquals(Rejection(PlacementProblem.OTHER_ACTOR_TYPE_NOT_ALLOWED), context.rejection(SetEventOtherActorType, SetEventOtherActorType.Input("Ball", 0, "Wall")))
        assertEquals(Rejection(LookupProblem.ACTOR_TYPE_MISSING, "Ghost"), context.rejection(SetEventOtherActorType, SetEventOtherActorType.Input("Ball", 2, "Ghost")))
        assertEquals(Rejection(PlacementProblem.KEY_NOT_ALLOWED), context.rejection(AddEvent, AddEvent.Input("Ball", EventType.EVERY_FRAME, key = KeyChoice.A)))
        assertEquals(Rejection(LookupProblem.GAME_KEY_UNKNOWN, "Hyper"), context.rejection(SetEventKey, SetEventKey.Input("Ball", 1, KeyChoice("Hyper"))))
        assertEquals(Rejection(LookupProblem.GAME_KEY_UNKNOWN, "Hyper"), context.rejection(AddEvent, AddEvent.Input("Ball", EventType.KEY_PRESSED, key = KeyChoice("Hyper"))))
        assertEquals(Rejection(LookupProblem.GAME_KEY_UNKNOWN, "Hyper"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice("Hyper"))))))
        assertEquals(KeyChoice("F5"), context.assertRoundTrip(Invocation(SetEventKey, SetEventKey.Input("Ball", 1, KeyChoice("F5")))) { context.ball() }!!.events[1].key)
    }

    @Test
    fun blocksAreAddedEditedMovedAndRemoved() = runTest {
        val context = commandContext(ball, wall)
        val bounce = BlockDefinition.BounceOffOther
        assertEquals(listOf(bounce, BlockDefinition.RemoveOther), context.assertRoundTrip(Invocation(AddBlock, AddBlock.Input("Ball", 2, bounce, index = 0))) { context.ball() }!!.events[2].blocks)
        assertEquals(listOf(BlockDefinition.SetVelocity(3f, 4f), jump), context.assertRoundTrip(Invocation(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.SetVelocity(3f, 4f)))) { context.ball() }!!.events[0].blocks)
        assertEquals(listOf(jump, BlockDefinition.SetVelocity(3f, 4f)), context.assertRoundTrip(Invocation(MoveBlock, MoveBlock.Input("Ball", 0, 0, 1))) { context.ball() }!!.events[0].blocks)
        assertEquals(listOf(BlockDefinition.SetVelocity(3f, 4f)), context.assertRoundTrip(Invocation(RemoveBlock, RemoveBlock.Input("Ball", 0, 0))) { context.ball() }!!.events[0].blocks)
    }

    @Test
    fun audioBlocksMayLeaveTheirFileUnchosenButNotNameOneThatDoesNotExist() = runTest {
        val context = commandContext(ball, wall, audioFiles = AudioFiles(sounds = listOf("files/sounds/hit.wav"), music = listOf("files/music/theme.mp3")))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.PlaySound())))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.PlaySound("files/sounds/hit.wav"))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.PlayMusic("files/music/theme.mp3", isLooping = false))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.StopMusic)))
        assertEquals(Rejection(LookupProblem.SOUND_MISSING, "files/music/theme.mp3"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.PlaySound("files/music/theme.mp3"))))
        assertEquals(Rejection(LookupProblem.MUSIC_MISSING, "files/sounds/hit.wav"), context.rejection(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.PlayMusic("files/sounds/hit.wav"))))
        assertNull(commandContext(ball).rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.PlaySound("files/sounds/unknown.wav"))))
    }

    @Test
    fun goToSceneBlocksMayLeaveTheSceneUnchosenButNotNameOneThatDoesNotExist() = runTest {
        val root = createTempDirectory().toFile()
        File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/level-2.json").apply { parentFile.mkdirs() }.writeText("[]")
        val context = commandContext(ball, wall, rootDirectory = root)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.GoToScene())))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.GoToScene("level-2"))))
        assertEquals(Rejection(LookupProblem.SCENE_MISSING, "ghost"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.GoToScene("ghost"))))
        assertEquals(Rejection(LookupProblem.SCENE_MISSING, "ghost"), context.rejection(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.GoToScene("ghost"))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.RestartScene)))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.PauseGame(PauseAction.RESUME))))
        root.deleteRecursively()
    }

    @Test
    fun propertyBlocksMayLeaveThePropertyUnchosenButNotNameOneTheActorTypeLacks() = runTest {
        val context = commandContext(ball.copy(properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Toggle("isFast"))), wall)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.ChangeNumber())))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.ChangeNumber("score"))))
        assertEquals(Rejection(PlacementProblem.NUMBER_PROPERTY_MISSING, "lives", "Ball"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.SetNumber("lives"))))
        assertEquals(Rejection(PlacementProblem.TEXT_PROPERTY_MISSING, "isFast", "Ball"), context.rejection(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.SetText("isFast"))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.SetNumber("score", Float.NaN))))
        assertEquals(
            Rejection(NameProblem.PROPERTY_TAKEN),
            context.rejection(CreateActorType, ActorTypeDefinition("Paddle", properties = listOf(PropertyDefinition.Number("speed"), PropertyDefinition.Text("Speed".lowercase())))),
        )
    }

    @Test
    fun timerEventsStartWithTheDefaultTimerAndTheirTimerCanBeChanged() = runTest {
        val context = commandContext(ball, wall)
        val added = context.assertRoundTrip(Invocation(AddEvent, AddEvent.Input("Ball", EventType.TIMER_FINISHED))) { context.ball() }!!.events.last()
        assertEquals(EventDefinition(EventType.TIMER_FINISHED, timer = "timer"), added)
        assertEquals("spawnEnemy", context.assertRoundTrip(Invocation(SetEventTimer, SetEventTimer.Input("Ball", 3, "spawnEnemy"))) { context.ball() }!!.events[3].timer)
        assertEquals(Rejection(PlacementProblem.TIMER_NOT_ALLOWED), context.rejection(SetEventTimer, SetEventTimer.Input("Ball", 0, "timer2")))
        assertEquals(Rejection(NameProblem.TIMER_FIRST_CHARACTER), context.rejection(SetEventTimer, SetEventTimer.Input("Ball", 3, "Spawn")))
        assertEquals(Rejection(NameProblem.NAME_KEYWORD), context.rejection(SetEventTimer, SetEventTimer.Input("Ball", 3, "while")))
        assertEquals(Rejection(PlacementProblem.TIMER_NOT_ALLOWED), context.rejection(AddEvent, AddEvent.Input("Ball", EventType.EVERY_FRAME, timer = "timer2")))
        assertEquals(Rejection(NameProblem.NAME_EMPTY), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.StopTimer(timer = ""))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.StartTimer(seconds = -1f))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.StartTimer(timer = "timer9", seconds = 0f, isRepeating = true))))
    }

    @Test
    fun blocksThatNameAnActorTypeNeedOneThatExistsAndFollowItsRename() = runTest {
        val context = commandContext(ball, wall)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.AddActor())))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.MoveTowardsNearest("Wall"))))
        assertEquals(Rejection(LookupProblem.ACTOR_TYPE_MISSING, "Brick"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.AddActor("Brick"))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.AddActor("Wall", x = Float.NaN))))
        Invocation(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.AddActor("Wall", 1f, 2f))).apply(context)
        Invocation(RenameActorType, RenameActorType.Input("Wall", "Brick")).apply(context)
        assertEquals(BlockDefinition.AddActor("Brick", 1f, 2f), context.ball()!!.events[0].blocks.last())
    }

    @Test
    fun flowBlocksAreValidated() = runTest {
        val context = commandContext(ball.copy(properties = listOf(PropertyDefinition.Number("score"))), wall)
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.If(ConditionDefinition.Number("score", value = 3f)))))
        assertNull(context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.Repeat(times = 0))))
        assertEquals(Rejection(PlacementProblem.NUMBER_PROPERTY_MISSING, "lives", "Ball"), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.If(ConditionDefinition.Number("lives")))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.If(ConditionDefinition.Chance(101f)))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.If(ConditionDefinition.Velocity(value = Float.POSITIVE_INFINITY)))))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.Repeat(times = -1))))
    }

    @Test
    fun blockChangesAreValidated() = runTest {
        val context = commandContext(ball, wall)
        assertEquals(Rejection(PlacementProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION), context.rejection(AddBlock, AddBlock.Input("Ball", 0, BlockDefinition.RemoveOther)))
        assertEquals(Rejection(PlacementProblem.BLOCK_BOUNCE_OFF_EDGE_OUTSIDE_EDGE_TOUCHED), context.rejection(EditBlock, EditBlock.Input("Ball", 0, 0, BlockDefinition.BounceOffEdge)))
        assertEquals(Rejection(LookupProblem.BLOCK_MISSING_NONE, "Ball", "1", "0"), context.rejection(RemoveBlock, RemoveBlock.Input("Ball", 1, 0)))
        assertEquals(Rejection(LookupProblem.INDEX_INVALID, "2", "1"), context.rejection(MoveBlock, MoveBlock.Input("Ball", 0, 0, 2)))
        assertEquals(EditBlock.mergeKey(EditBlock.Input("Ball", 0, 1, jump)), EditBlock.mergeKey(EditBlock.Input("Ball", 0, 1, stop)))
    }

    @Test
    fun targetsNameWhatACommandCreatedOrMoved() = runTest {
        val context = commandContext(ball, wall)
        val addBlock = AddBlock.Input("Ball", 0, stop)
        AddBlock.apply(context, addBlock)
        assertEquals(CommandTarget.BlockType("Ball", 0, 2), AddBlock.target(context, addBlock))
        assertEquals(CommandTarget.BlockType("Ball", 0, 0), AddBlock.target(context, addBlock.copy(index = 0)))
        assertEquals(CommandTarget.BlockType("Ball", 2), MoveEvent.target(context, MoveEvent.Input("Ball", 0, 2)))
        assertEquals(CommandTarget.BlockType("Ball", 0, 1), MoveBlock.target(context, MoveBlock.Input("Ball", 0, 0, 1)))
        val rename = RenameActorType.Input("Wall", "Brick")
        assertEquals(CommandTarget.BlockType("Wall"), RenameActorType.target(context, rename))
        RenameActorType.apply(context, rename)
        assertEquals(CommandTarget.BlockType("Brick"), RenameActorType.target(context, rename))
    }

    @Test
    fun onlyCodeChangedByHandCanBeDiscardedAndThatCantBeUndone() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball)).apply { changedByHand = setOf("Ball") }
        val context = commandContext(blockTypes = blockTypesController(repository = repository))
        assertNull(context.rejection(DiscardChangesMadeByHand, DiscardChangesMadeByHand.Input("Ball")))
        assertNull(DiscardChangesMadeByHand.apply(context, DiscardChangesMadeByHand.Input("Ball")).inverse)
        assertEquals("regenerate overwriting Ball", repository.writes.last())
        assertEquals(Rejection(CommandProblem.NOT_CHANGED_BY_HAND, "Ball"), context.rejection(DiscardChangesMadeByHand, DiscardChangesMadeByHand.Input("Ball")))
    }

    @Test
    fun aRenameShowsBeforeItsScenesAreWritten() = runTest {
        val speed = PropertyDefinition.Number("speed")
        val context = commandContext(ball.copy(properties = listOf(speed)), wall, ioDispatcher = StandardTestDispatcher(testScheduler))
        val executor = CommandExecutor(context, testScope())
        executor.dispatch(Invocation(RenameActorType, RenameActorType.Input("Ball", "Puck")))
        assertNotNull(context.blockTypes!!["Puck"])
        testScheduler.runCurrent()
        assertEquals(1, executor.history.value.undoable.size)
        executor.dispatch(Invocation(EditProperty, EditProperty.Input("Puck", "speed", speed.copy(name = "pace"))))
        assertEquals(listOf(speed.copy(name = "pace")), context.blockTypes["Puck"]!!.properties)
        testScheduler.runCurrent()
    }

    @Test
    @OptIn(EditorMutation::class)
    fun aRenameWhoseSceneCantBeWrittenChangesNothing() = runTest {
        var isLocked = false
        val context = commandContext(ball, wall, sceneWriteText = { file, text -> if (isLocked && file.name == "two.json") throw IOException("Locked.") else file.writeTextAtomically(text) })
        listOf("one", "two").forEach { name -> context.scenes.write(name, listOf(SceneActorEntry("Ball", "{}", name))) }
        val scenes = listOf("one", "two").map { context.scenes.read(it) }
        isLocked = true
        val executor = CommandExecutor(context, testScope())
        assertEquals(
            CommandOutcome.Rejected(Rejection(CommandProblem.FAILED, "Locked.")),
            executor.execute(Invocation(RenameActorType, RenameActorType.Input("Ball", "Puck"))),
        )
        assertEquals(ball, context.ball())
        assertNull(context.blockTypes!!["Puck"])
        assertEquals(scenes, listOf("one", "two").map { File(context.scenes.fileOf(it).path).readText() })
        assertEquals(0, executor.history.value.undoable.size)
    }
}
