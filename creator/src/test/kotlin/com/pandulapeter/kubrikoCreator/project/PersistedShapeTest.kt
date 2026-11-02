/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins what Kubriko Creator persists in `.kubriko` type files to the format version, by rendering the serial descriptors of
 * the type definitions (every field, sealed subclass, and enum value) and comparing them with the file of the current
 * format version. Defaults aren't part of a descriptor: the golden JSON fixtures, which write every default, cover those.
 */
@OptIn(ExperimentalSerializationApi::class)
class PersistedShapeTest {

    @Test
    fun thePersistedShapeIsTheOneOfTheCurrentFormatVersion() {
        val fileName = "persisted-shape-${ProjectFormat.CURRENT_VERSION}.txt"
        val expected = assertNotNull(javaClass.getResource("/formats/$fileName"), "$fileName is missing.").readText().replace("\r\n", "\n")
        assertEquals(
            expected,
            renderedShape(),
            "The persisted format changed. Raise the format version (an empty FormatMigration in ProjectFormat.MIGRATIONS) " +
                "and add persisted-shape-${ProjectFormat.CURRENT_VERSION + 1}.txt; never edit the file of a released version.",
        )
    }

    @Test
    fun everyBlockEventConditionAndPropertyKindIsInTheShape() {
        val shape = renderedShape().lines().toSet()
        val blockDescriptor = BlockDefinition.serializer().descriptor
        BlockKind.entries.forEach { kind ->
            val blockName = Json.encodeToJsonElement(BlockDefinition.serializer(), kind.createDefault()).jsonObject.getValue("type").jsonPrimitive.content
            assertTrue("${blockDescriptor.serialName} : $blockName" in shape, "$blockName is missing from the shape.")
        }
        val eventDescriptor = EventType.serializer().descriptor
        (0 until eventDescriptor.elementsCount).forEach { index ->
            val line = "${eventDescriptor.serialName} = ${eventDescriptor.getElementName(index)}"
            assertTrue(line in shape, "$line is missing from the shape.")
        }
    }

    private fun renderedShape(): String {
        val lines = sortedSetOf<String>()
        val visited = mutableSetOf<String>()
        fun walk(descriptor: SerialDescriptor, name: String = descriptor.serialName.removeSuffix("?")) {
            val isCollection = descriptor.kind == StructureKind.LIST || descriptor.kind == StructureKind.MAP
            if (!isCollection && !visited.add(name)) return
            when (descriptor.kind) {
                PolymorphicKind.SEALED -> {
                    val subclasses = descriptor.getElementDescriptor(1)
                    (0 until subclasses.elementsCount).forEach { index ->
                        val subclassName = subclasses.getElementName(index)
                        lines += "$name : $subclassName"
                        walk(subclasses.getElementDescriptor(index), "$name:$subclassName")
                    }
                }
                SerialKind.ENUM -> (0 until descriptor.elementsCount).forEach { lines += "$name = ${descriptor.getElementName(it)}" }
                StructureKind.CLASS, StructureKind.OBJECT -> (0 until descriptor.elementsCount).forEach { index ->
                    val element = descriptor.getElementDescriptor(index)
                    val nullable = if (element.isNullable) "?" else ""
                    val optional = if (descriptor.isElementOptional(index)) "=" else ""
                    lines += "$name.${descriptor.getElementName(index)}: ${element.typeName()}$nullable$optional"
                    walk(element)
                }
                StructureKind.LIST, StructureKind.MAP -> (0 until descriptor.elementsCount).forEach { walk(descriptor.getElementDescriptor(it)) }
                else -> Unit
            }
        }
        walk(ActorTypeDefinition.serializer().descriptor)
        walk(ManagerTypeDefinition.serializer().descriptor)
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun SerialDescriptor.typeName(): String = when (kind) {
        StructureKind.LIST, StructureKind.MAP -> (0 until elementsCount).joinToString(", ", "${serialName.removeSuffix("?")}<", ">") { index ->
            getElementDescriptor(index).let { it.typeName() + if (it.isNullable) "?" else "" }
        }
        else -> serialName.removeSuffix("?")
    }
}
