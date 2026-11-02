/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DiscardChangesMadeByHand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameTimer
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventKey
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventOtherActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventTimer
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetParent
import com.pandulapeter.kubrikoCreator.commands.managerTypes.AddText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.EditText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RemoveText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.SetKotlinUi
import com.pandulapeter.kubrikoCreator.commands.project.SetDescription

/**
 * The commands the type editor of [definition] dispatches through [onCommand], one function per thing it changes.
 */
internal class BlockTypeEditorActions(
    private val definition: BlockTypeDefinition,
    private val onCommand: (Invocation<*>) -> Unit,
) {
    private val name get() = definition.name

    fun delete() = when (definition) {
        is ActorTypeDefinition -> onCommand(Invocation(DeleteActorType, DeleteActorType.Input(name)))
        is ManagerTypeDefinition -> onCommand(Invocation(DeleteManagerType, DeleteManagerType.Input(name)))
    }

    fun setParent(parent: String?) = onCommand(Invocation(SetParent, SetParent.Input(name, parent)))

    fun setDescription(description: String) = onCommand(Invocation(SetDescription, SetDescription.Input(type = name, description = description)))

    fun discardChangesMadeByHand() = onCommand(Invocation(DiscardChangesMadeByHand, DiscardChangesMadeByHand.Input(name)))

    fun setAppearance(input: SetAppearance.Input) = onCommand(Invocation(SetAppearance, input))

    fun addProperty(property: PropertyDefinition) = onCommand(Invocation(AddProperty, AddProperty.Input(name, property)))

    fun editProperty(propertyName: String, property: PropertyDefinition) = onCommand(Invocation(EditProperty, EditProperty.Input(name, propertyName, property)))

    fun removeProperty(propertyName: String) = onCommand(Invocation(RemoveProperty, RemoveProperty.Input(name, propertyName)))

    fun addText() = onCommand(Invocation(AddText, AddText.Input(name, ScreenTextDefinition.defaultFor(definition.properties))))

    fun editText(index: Int, text: ScreenTextDefinition) = onCommand(Invocation(EditText, EditText.Input(name, index, text)))

    fun removeText(index: Int) = onCommand(Invocation(RemoveText, RemoveText.Input(name, index)))

    fun setKotlinUi(code: String) = onCommand(Invocation(SetKotlinUi, SetKotlinUi.Input(name, code)))

    fun addEvent(type: EventType) = onCommand(Invocation(AddEvent, AddEvent.Input(name, type)))

    fun overrideEvent(event: EventDefinition) = onCommand(
        Invocation(AddEvent, AddEvent.Input(name, event.type, event.key, event.otherActorType, event.timer, listOf(BlockDefinition.RunParentEvent))),
    )

    fun moveEvent(index: Int, targetIndex: Int) = onCommand(Invocation(MoveEvent, MoveEvent.Input(name, index, targetIndex)))

    fun removeEvent(index: Int) = onCommand(Invocation(RemoveEvent, RemoveEvent.Input(name, index)))

    fun setEventKey(eventIndex: Int, key: KeyChoice) = onCommand(Invocation(SetEventKey, SetEventKey.Input(name, eventIndex, key)))

    fun setEventTimer(eventIndex: Int, timer: String) = onCommand(Invocation(SetEventTimer, SetEventTimer.Input(name, eventIndex, timer)))

    fun setEventOtherActorType(eventIndex: Int, other: String) = onCommand(
        Invocation(SetEventOtherActorType, SetEventOtherActorType.Input(name, eventIndex, other)),
    )

    fun renameTimer(oldName: String, newName: String) = onCommand(Invocation(RenameTimer, RenameTimer.Input(name, oldName, newName)))

    fun addBlock(eventIndex: Int, kind: BlockKind, gap: Int? = null) = onCommand(
        Invocation(AddBlock, AddBlock.Input(name, eventIndex, kind.createDefault(), index = gap)),
    )

    fun moveBlock(eventIndex: Int, index: Int, gap: Int) {
        moveTarget(index, gap)?.let { onCommand(Invocation(MoveBlock, MoveBlock.Input(name, eventIndex, index, it))) }
    }

    fun editBlock(eventIndex: Int, blockIndex: Int, block: BlockDefinition) = onCommand(Invocation(EditBlock, EditBlock.Input(name, eventIndex, blockIndex, block)))

    fun removeBlock(eventIndex: Int, blockIndex: Int) = onCommand(Invocation(RemoveBlock, RemoveBlock.Input(name, eventIndex, blockIndex)))
}
