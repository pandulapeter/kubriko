/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.GeneratedNames
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind

/**
 * What a Scene gives the Properties of a Manager type when it starts: an Actor per Manager type with Properties, placed in
 * a Scene like any other, whose type ID is the Manager type's name, so renaming or deleting the Manager type reaches its
 * entries as it reaches an Actor type's. Only the values set are saved, and the others keep what the Manager has.
 *
 * Each class is nested in [GeneratedNames.ACTOR_TYPES] under the Manager type's own name: that object's code names only
 * Actor types otherwise, and no Actor type can share a Manager type's name, so the classes need no reserved names of their
 * own. They name the Manager in full for the same reason. The State's fields are numbered rather than named after the
 * Properties, which could be called like its members.
 */
internal object ManagerSceneValuesCode {

    /**
     * The Manager types of [managerTypes] that a Scene can give values to: those with Properties.
     */
    fun typesWithValues(managerTypes: List<ManagerTypeDefinition>) = managerTypes.filter { it.properties.isNotEmpty() }.sortedBy { it.name }

    /**
     * What the classes of [managerTypes] import: Kubriko's serializer of colors too if a Scene can give one.
     */
    fun importsOf(managerTypes: List<ManagerTypeDefinition>) = imports + if (managerTypes.any { type -> type.properties.any { it is PropertyDefinition.Color } }) {
        listOf("com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor")
    } else {
        emptyList()
    }

    private val imports = listOf(
        "com.pandulapeter.kubriko.Kubriko",
        "com.pandulapeter.kubriko.actor.Actor",
        "com.pandulapeter.kubriko.actor.traits.Unique",
        "com.pandulapeter.kubriko.helpers.extensions.get",
        "com.pandulapeter.kubriko.sceneEditor.Editable",
        "com.pandulapeter.kubriko.serialization.Serializable",
        "kotlinx.serialization.SerialName",
        "kotlinx.serialization.json.Json",
    )

    fun StringBuilder.appendSceneValues(definition: ManagerTypeDefinition, packageName: String) {
        val name = definition.name
        appendLine()
        appendLine("    /**")
        appendLine("     * What the Scene it is placed in gives the Properties of $name when it starts; those it doesn't name keep theirs.")
        appendLine("     */")
        appendLine("    class $name private constructor(private val state: State) : Actor, Unique, Editable<$name> {")
        appendLine("        override val body = com.pandulapeter.kubriko.actor.body.PointBody()")
        appendLine()
        appendLine("        override fun onAdded(kubriko: Kubriko) {")
        appendLine("            if (kubriko.get<${GeneratedNames.SCENES}>().isInSceneEditor) return")
        appendLine("            val manager = kubriko.get<$packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}.$name>()")
        definition.properties.forEachIndexed { index, property -> appendLine("            state.${fieldOf(index)}?.let { manager.${property.name} = it }") }
        appendLine("        }")
        appendLine()
        appendLine("        override fun save() = state")
        appendLine()
        appendLine("        @kotlinx.serialization.Serializable")
        appendLine("        data class State(")
        definition.properties.forEachIndexed { index, property ->
            appendLine("            @SerialName(\"${property.name}\") val ${fieldOf(index)}: ${PropertyKind.of(property).savedType}? = null,")
        }
        appendLine("        ) : Serializable.State<$name> {")
        appendLine()
        appendLine("            override fun restore() = $name(this)")
        appendLine()
        appendLine("            override fun serialize() = Json.encodeToString(this)")
        appendLine("        }")
        appendLine()
        appendLine("        companion object {")
        appendLine("            val metadata = EditableMetadata.create<$name, State>(typeId = \"$name\") { State() }")
        appendLine("        }")
        appendLine("    }")
    }

    private fun fieldOf(index: Int) = "value${index + 1}"
}
