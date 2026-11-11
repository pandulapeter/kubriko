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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EveryBlockTypes
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyPlaceholders
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.actorTypeReference
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.timerNames
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteTimer
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveProperty
import com.pandulapeter.kubrikoCreator.commands.folders.DeleteFolder
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.resources.DeleteResource
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Deletes each thing [EveryBlockTypes] has, one at a time, and checks that no Block, Event, or placeholder names it
 * afterwards, by reading the Blocks themselves rather than through the usage finder, so a usage it misses fails here.
 */
class DeletingEveryThingTest {

    @Test
    fun noActorTypeOrManagerTypeIsNamedOnceDeleted() = runTest {
        val deletable = EveryBlockTypes.all.filter { type -> EveryBlockTypes.all.none { (it as? ActorTypeDefinition)?.parent == type.name } }
        deletable.forEach { type ->
            val context = commandContext(*EveryBlockTypes.all.toTypedArray())
            val invocation = when (type) {
                is ActorTypeDefinition -> Invocation(DeleteActorType, DeleteActorType.Input(type.name))
                is ManagerTypeDefinition -> Invocation(DeleteManagerType, DeleteManagerType.Input(type.name))
            }
            assertNull(invocation.validate(context))
            assertNull(invocation.apply(context).failure)
            val left = context.blockTypes!!.types.value
            assertEquals(EveryBlockTypes.all.size - 1, left.size)
            left.forEach { other ->
                other.events.forEach { event ->
                    assertTrue(event.otherActorType != type.name, "${other.name} still collides with ${type.name}.")
                    event.blocks.forEach { block ->
                        assertTrue(block.actorTypeReference != type.name, "${other.name} still has $block.")
                        assertTrue(block.propertyReference?.manager != type.name, "${other.name} still has $block.")
                        if (block is BlockDefinition.Log) assertTrue(PropertyPlaceholders.find(block.message).none { it.manager == type.name }, "${other.name} still has $block.")
                    }
                }
                (other as? ManagerTypeDefinition)?.texts?.forEach { text -> assertTrue(PropertyPlaceholders.find(text.text).none { it.manager == type.name }, "${other.name} still shows $text.") }
            }
        }
    }

    @Test
    fun noActorTypeIsNamedOnceItsFolderIsDeleted() = runTest {
        val types = EveryBlockTypes.all.map { if (it is ActorTypeDefinition) it.copy(folder = "cast") else it }
        val context = commandContext(*types.toTypedArray())
        val invocation = Invocation(DeleteFolder, DeleteFolder.Input(FolderGroup.ACTOR_TYPES, "cast"))
        assertNull(invocation.validate(context))
        assertNull(invocation.apply(context).failure)
        val deleted = types.filterIsInstance<ActorTypeDefinition>().mapTo(HashSet()) { it.name }
        assertEquals(types.size - deleted.size, context.blockTypes!!.types.value.size)
        context.blockTypes!!.types.value.forEach { other ->
            other.events.forEach { event ->
                assertTrue(event.otherActorType !in deleted, "${other.name} still collides with ${event.otherActorType}.")
                event.blocks.forEach { block -> assertTrue(block.actorTypeReference !in deleted, "${other.name} still has $block.") }
            }
        }
    }

    @Test
    fun noPropertyIsNamedOnceRemoved() = runTest {
        EveryBlockTypes.all.forEach { type ->
            type.properties.forEach { property ->
                val context = commandContext(*EveryBlockTypes.all.toTypedArray())
                val invocation = Invocation(RemoveProperty, RemoveProperty.Input(type.name, property.name))
                assertNull(invocation.validate(context))
                assertNull(invocation.apply(context).failure)
                val left = context.blockTypes!!.types.value
                val family = ActorTypeFamily(left.filterIsInstance<ActorTypeDefinition>())
                fun isRemoved(user: BlockTypeDefinition, manager: String?, name: String) = name == property.name && when (manager) {
                    null -> user.name == type.name || (type is ActorTypeDefinition && type.name in family.ancestorsOf(user.name) && family.declarerOf(user.name, name) == null)
                    else -> manager == type.name
                }
                left.forEach { user ->
                    user.events.forEach { event ->
                        event.blocks.forEach { block ->
                            val reference = block.propertyReference
                            assertTrue(reference == null || !isRemoved(user, reference.manager, reference.name), "${user.name} still has $block after removing ${type.name}.${property.name}.")
                            if (block is BlockDefinition.Log) assertTrue(PropertyPlaceholders.find(block.message).none { isRemoved(user, it.manager, it.name) }, "${user.name} still has $block.")
                        }
                    }
                    (user as? ManagerTypeDefinition)?.texts?.forEach { text -> assertTrue(PropertyPlaceholders.find(text.text).none { isRemoved(user, it.manager, it.name) }, "${user.name} still shows $text.") }
                }
            }
        }
    }

    @Test
    fun noSceneIsNamedOnceDeleted() = runTest {
        val context = commandContext(*EveryBlockTypes.all.toTypedArray())
        File(context.project.rootDirectory, "src/commonMain/composeResources/files/scenes/main.json").apply { parentFile.mkdirs() }.writeText("[]")
        context.scenes.refresh()
        val invocation = Invocation(DeleteScene, DeleteScene.Input("main"))
        assertNull(invocation.validate(context))
        assertNull(invocation.apply(context).failure)
        context.blockTypes!!.types.value.forEach { type ->
            type.events.forEach { event -> event.blocks.forEach { block -> assertTrue(block != BlockDefinition.GoToScene("main"), "${type.name} still goes to main.") } }
        }
    }

    @Test
    fun noSpriteSoundOrMusicIsUsedOnceDeleted() = runTest {
        val types = EveryBlockTypes.all
        val paths = types.flatMap { type ->
            listOfNotNull((type as? ActorTypeDefinition)?.sprite) + type.events.flatMap { event ->
                event.blocks.mapNotNull { (it as? BlockDefinition.PlaySound)?.sound ?: (it as? BlockDefinition.PlayMusic)?.music }
            }
        }.filter { ResourceKind.of(it) != null }.distinct()
        assertTrue(paths.size >= 3, "EveryBlockTypes uses a sprite, a sound, and music.")
        paths.forEach { path ->
            val context = commandContext(*types.toTypedArray())
            File(context.project.rootDirectory, "src/commonMain/composeResources/$path").apply { parentFile.mkdirs() }.writeBytes(byteArrayOf(1))
            val invocation = Invocation(DeleteResource, DeleteResource.Input(path))
            assertNull(invocation.validate(context))
            assertNull(invocation.apply(context).failure)
            context.blockTypes!!.types.value.forEach { type ->
                assertTrue((type as? ActorTypeDefinition)?.sprite != path, "${type.name} is still drawn as $path.")
                type.events.forEach { event ->
                    event.blocks.forEach { block ->
                        assertTrue(block != BlockDefinition.PlaySound(path) && (block as? BlockDefinition.PlayMusic)?.music != path, "${type.name} still has $block.")
                    }
                }
            }
        }
    }

    @Test
    fun noTimerIsUsedOnceDeleted() = runTest {
        val types = EveryBlockTypes.all
        val timers = types.flatMap { type -> type.timerNames.map { type.name to it } }
        assertTrue(timers.isNotEmpty(), "EveryBlockTypes uses Timers.")
        timers.forEach { (typeName, timer) ->
            val context = commandContext(*types.toTypedArray())
            val invocation = Invocation(DeleteTimer, DeleteTimer.Input(typeName, timer))
            assertNull(invocation.validate(context))
            assertNull(invocation.apply(context).failure)
            val family = ActorTypeFamily(context.blockTypes!!.actorTypes.value)
            val sharers = if (types.first { it.name == typeName } is ActorTypeDefinition) family.familyOf(family.rootOf(typeName)) else listOf(typeName)
            context.blockTypes!!.types.value.filter { it.name in sharers }.forEach { type ->
                assertTrue(timer !in type.timerNames, "${type.name} still uses the Timer $timer.")
            }
        }
    }
}
