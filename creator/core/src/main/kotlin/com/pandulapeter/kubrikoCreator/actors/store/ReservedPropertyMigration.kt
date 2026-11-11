/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.actors.withManagerPropertyRenamed
import com.pandulapeter.kubrikoCreator.actors.withPropertyReplaced
import com.pandulapeter.kubrikoCreator.actors.withPropertyUsesRenamed
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.FormatMigration
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.withValueRenamed
import java.io.File

/**
 * A raise of the format that reserves Property names the generated classes now use ([PropertyNaming]): a Property called
 * one of [reservedNames] is renamed to the first free name after it (`look2`) in the type that declares it, and, as
 * renaming it in Kubriko Creator does, in the Blocks, texts, and Kotlin of every type that uses it and in the values the
 * Actors of its family keep in the Scenes. The Scenes are changed first, so a Scene that can't be written leaves every
 * type as it was for the next try. The types are saved after them, declarers last, so the one rare failure left (a
 * declarer that can't be saved after another of its family was) can only leave a family whose Property has two names
 * until it is renamed in Kubriko Creator. Type files that can't be read are left as they are.
 *
 * Format version 3 reserved `look`, the member generated Actor types are drawn through ([LOOK]), and format version 4
 * the members that play a sprite cut into frames ([ANIMATION]), whose version also added the animation Blocks and
 * tinting a sprite.
 */
internal class ReservedPropertyMigration(private vararg val reservedNames: String) : FormatMigration {

    override fun migrate(rootDirectory: File) = reservedNames.forEach { migrate(rootDirectory, it) }

    private fun migrate(rootDirectory: File, reservedName: String) {
        val store = BlockTypeStore.open(rootDirectory) ?: return
        val types = store.load(isCheckingPropertyNames = false).types
        val renamedTypes = LinkedHashMap<String, BlockTypeDefinition>()
        fun current(type: BlockTypeDefinition) = renamedTypes[type.name] ?: type
        fun put(type: BlockTypeDefinition, changed: BlockTypeDefinition) {
            if (changed != current(type)) renamedTypes[type.name] = changed
        }
        val newNamesByType = HashMap<String, String>()
        types.filterIsInstance<ManagerTypeDefinition>().filter { it.declares(reservedName) }.forEach { manager ->
            val newName = PropertyNaming.freeName(reservedName, manager.properties.map { it.name } - reservedName)
            put(manager, current(manager).withPropertyReplaced(reservedName, manager.propertyCalled(reservedName).withName(newName)))
            types.forEach { type -> put(type, current(type).withManagerPropertyRenamed(manager.name, reservedName, newName)) }
        }
        val actorTypes = types.filterIsInstance<ActorTypeDefinition>()
        val family = ActorTypeFamily(actorTypes)
        val byName = actorTypes.associateBy { it.name }
        actorTypes.map { family.rootOf(it.name) }.distinct().forEach { root ->
            val members = family.familyOf(root).mapNotNull(byName::get)
            if (members.none { it.declares(reservedName) }) return@forEach
            val newName = PropertyNaming.freeName(reservedName, members.flatMap { member -> member.properties.map { it.name } } - reservedName)
            val users = members.filter { member -> family.propertiesOf(member.name).any { it.name == reservedName } }
            users.forEach { member ->
                put(
                    member,
                    if (member.declares(reservedName)) {
                        current(member).withPropertyReplaced(reservedName, member.propertyCalled(reservedName).withName(newName))
                    } else {
                        current(member).withPropertyUsesRenamed(reservedName, newName)
                    },
                )
            }
            users.forEach { newNamesByType[it.name] = newName }
        }
        if (newNamesByType.isNotEmpty()) renameSceneValues(rootDirectory, reservedName, newNamesByType)
        val (declarers, users) = types.filter { it.name in renamedTypes }.partition { it.declares(reservedName) }
        (users + declarers).forEach { store.save(renamedTypes.getValue(it.name)) }
    }

    /**
     * Renames the value [reservedName] in the states of the Actors of the types in [newNamesByType] to the type's new
     * name, in every Scene file that can be read.
     */
    private fun renameSceneValues(rootDirectory: File, reservedName: String, newNamesByType: Map<String, String>) {
        ResourceScanner.scan(rootDirectory)[ResourceGroup.SCENES].orEmpty().forEach { resource ->
            val actors = try {
                SceneDocument.parse(resource.file.readText())
            } catch (_: Exception) {
                return@forEach
            }
            val renamed = actors.map { actor ->
                newNamesByType[actor.typeId]?.let { newName ->
                    SceneDocument.withStateChanged(actor.state) { it.withValueRenamed(reservedName, newName) }?.let { actor.copy(state = it) }
                } ?: actor
            }
            if (renamed != actors) resource.file.writeTextAtomically(SceneDocument.format(renamed))
        }
    }

    private fun BlockTypeDefinition.declares(reservedName: String) = properties.any { it.name == reservedName }

    private fun BlockTypeDefinition.propertyCalled(reservedName: String) = properties.first { it.name == reservedName }

    companion object {
        val LOOK = ReservedPropertyMigration("look")
        val ANIMATION = ReservedPropertyMigration("animation", "animationSpeed", "isAnimationLooping")
    }
}
