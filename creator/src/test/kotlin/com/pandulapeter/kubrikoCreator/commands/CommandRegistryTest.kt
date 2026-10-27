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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Keeps the commands, the AI tools generated from them, and the user interface in sync.
 */
class CommandRegistryTest {
    private val registry = CommandRegistry.editor

    @Test
    fun everyCommandClassIsRegistered() {
        val classesDirectory = File(EditorCommand::class.java.protectionDomain.codeSource.location.toURI())
        val commandClasses = classesDirectory.walkTopDown()
            .filter { it.extension == "class" }
            .map { it.relativeTo(classesDirectory).invariantSeparatorsPath.removeSuffix(".class").replace('/', '.') }
            .filter { it.startsWith("com.pandulapeter.kubrikoCreator.") }
            .map { Class.forName(it, false, javaClass.classLoader) }
            .filter { EditorCommand::class.java.isAssignableFrom(it) && !it.isInterface && !Modifier.isAbstract(it.modifiers) }
            .toSet()
        assertTrue(commandClasses.isNotEmpty())
        assertEquals(commandClasses.map { it.name }.sorted(), registry.commands.map { it.javaClass.name }.sorted())
    }

    @Test
    fun theToolListIsTheCommandList() {
        assertEquals(registry.commands.map { it.name }, registry.tools.map { it.name })
        assertEquals(registry.commands.map { it.description }, registry.tools.map { it.description })
        assertEquals(registry.commands.map { it is ReadCommand }, registry.tools.map { it.isReadOnly })
        registry.commands.forEach { assertEquals(it, registry[it.name]) }
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
        val sources = File("src/main/kotlin/com/pandulapeter/kubrikoCreator")
        val optingIn = sources.walkTopDown()
            .filter { it.extension == "kt" && "OptIn(EditorMutation" in it.readText() }
            .map { it.relativeTo(sources).invariantSeparatorsPath }
            .filterNot { it.startsWith("commands/") }
            .toList()
        assertTrue(optingIn.isEmpty(), "Only commands may change the project: $optingIn")
    }

    private fun undescribedPaths(schema: JsonObject, path: String, isDescriptionRequired: Boolean): List<String> = buildList {
        if (isDescriptionRequired && "description" !in schema && "const" !in schema) add(path)
        schema["properties"]?.jsonObject?.forEach { (name, property) -> addAll(undescribedPaths(property.jsonObject, "$path.$name", isDescriptionRequired = true)) }
        schema["items"]?.let { addAll(undescribedPaths(it.jsonObject, "$path[]", isDescriptionRequired = false)) }
        (schema["oneOf"] as? JsonArray)?.forEachIndexed { index, option: JsonElement -> addAll(undescribedPaths(option.jsonObject, "$path|$index", isDescriptionRequired = true)) }
    }
}
