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

import com.pandulapeter.kubrikoCreator.commands.project.GetProjectDigest
import com.pandulapeter.kubrikoCreator.commands.screenplay.KeepPageImages
import com.pandulapeter.kubrikoCreator.commands.screenplay.MovePageToDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.RemovePage
import com.pandulapeter.kubrikoCreator.commands.screenplay.RestoreMarkdown
import com.pandulapeter.kubrikoCreator.commands.screenplay.UnlinkMentions
import com.pandulapeter.kubrikoCreator.testing.creatorSourceRoots
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Keeps the commands, the AI tools generated from them, and the user interface in sync.
 */
class CommandRegistryTest {
    private val registry = CommandRegistry.editor

    @Test
    fun everyCommandClassIsRegistered() {
        val commandClasses = classFilePaths(File(EditorCommand::class.java.protectionDomain.codeSource.location.toURI()))
            .map { it.removeSuffix(".class").replace('/', '.') }
            .filter { it.startsWith("com.pandulapeter.kubrikoCreator.") }
            .map { Class.forName(it, false, javaClass.classLoader) }
            .filter { EditorCommand::class.java.isAssignableFrom(it) && !it.isInterface && !Modifier.isAbstract(it.modifiers) }
            // Internal, not tools.
            .filter { it != CompositeCommand::class.java && it != ReplaceTypes::class.java && it != ReplaceScenes::class.java && it != MovePageToDocument::class.java && it != RestoreMarkdown::class.java && it != ReplaceFiles::class.java && it != RemovePage::class.java && it != UnlinkMentions::class.java && it != KeepPageImages::class.java }
            .toSet()
        assertTrue(commandClasses.isNotEmpty())
        assertEquals(commandClasses.map { it.name }.sorted(), registry.commands.map { it.javaClass.name }.sorted())
    }

    /**
     * The class files of [location], a classes folder or, on this module's test classpath (where the test fixtures put
     * it first), the module's jar.
     */
    private fun classFilePaths(location: File): List<String> = if (location.isDirectory) {
        location.walkTopDown().filter { it.extension == "class" }.map { it.relativeTo(location).invariantSeparatorsPath }.toList()
    } else {
        JarFile(location).use { jar -> jar.entries().asSequence().map { it.name }.filter { it.endsWith(".class") }.toList() }
    }

    @Test
    fun theToolListIsTheCommandList() {
        assertEquals(registry.commands.map { it.name }, registry.tools.map { it.name })
        assertEquals(registry.commands.map { it.description }, registry.tools.map { it.description })
        assertEquals(registry.commands.map { it is ReadCommand || (it is ActionCommand && it.leavesEverythingAsItWas) }, registry.tools.map { it.isReadOnly })
        assertEquals(registry.commands.map { it is ReadCommand }, registry.tools.map { it.readsProject })
        assertEquals(
            setOf("ask_user", "remember_for_next_time", "set_plan"),
            registry.tools.filter { it.isReadOnly && !it.readsProject }.map { it.name }.toSet(),
        )
        registry.commands.forEach { assertEquals(it, registry[it.name]) }
    }

    @Test
    fun onlyTheCoreToolsAreLoadedFromTheStart() {
        val alwaysLoaded = CommandRegistry.editor.tools.filter { it.isAlwaysLoaded }.map { it.name }
        assertEquals(CommandRegistry.editor.alwaysLoaded.map { it.name }.toSet(), alwaysLoaded.toSet())
        assertTrue("add_block" in alwaysLoaded && "press_keys" in alwaysLoaded)
        assertFalse("delete_actor_type" in alwaysLoaded)
        assertFailsWith<IllegalArgumentException> { CommandRegistry(commands = emptyList(), alwaysLoaded = setOf(GetProjectDigest)) }
    }

    @Test
    fun everyCommandHasAToolNameAndADescription() {
        registry.commands.forEach { command ->
            assertTrue(Regex("[a-z]+(_[a-z]+)*").matches(command.name), "${command.name} isn't a valid tool name.")
            assertTrue(command.description.endsWith("."), "The description of ${command.name} isn't a sentence.")
        }
    }

    @Test
    fun everyPartOfEveryToolSchemaIsDescribed() {
        registry.tools.forEach { tool ->
            assertEquals("object", tool.inputSchema["type"]?.jsonPrimitive?.content, tool.name)
            val undescribed = undescribedPaths(tool.inputSchema, tool.name, isDescriptionRequired = false)
            assertTrue(undescribed.isEmpty(), "Undescribed: $undescribed")
        }
    }

    @Test
    fun onlyCommandsChangeTheProject() {
        val optingIn = creatorSourceRoots().flatMap { root ->
            val sources = File(root, "com/pandulapeter/kubrikoCreator")
            sources.walkTopDown()
                .filter { it.extension == "kt" && "OptIn(EditorMutation" in it.readText() }
                .map { it.relativeTo(sources).invariantSeparatorsPath }
                .filterNot { it.startsWith("commands/") }
                .toList()
        }
        assertTrue(optingIn.isEmpty(), "Only commands may change the project: $optingIn")
    }

    private fun undescribedPaths(schema: JsonObject, path: String, isDescriptionRequired: Boolean): List<String> = buildList {
        if (isDescriptionRequired && "description" !in schema && "const" !in schema) add(path)
        schema["properties"]?.jsonObject?.forEach { (name, property) -> addAll(undescribedPaths(property.jsonObject, "$path.$name", isDescriptionRequired = true)) }
        schema["items"]?.let { addAll(undescribedPaths(it.jsonObject, "$path[]", isDescriptionRequired = false)) }
        (schema["oneOf"] as? JsonArray)?.forEachIndexed { index, option: JsonElement -> addAll(undescribedPaths(option.jsonObject, "$path|$index", isDescriptionRequired = true)) }
    }
}
