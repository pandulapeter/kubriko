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
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteTimer
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DiscardChangesMadeByHand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
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
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.SetKotlinUi
import com.pandulapeter.kubrikoCreator.commands.project.SetDescription

/**
 * The commands the type editor of [definition] dispatches through [onCommand], one function per thing it changes.
 * [onCommand] calls its `onSuccess` once the command was applied, never when it was rejected. Renames go through
 * [onCommandIfValid] instead, which returns why one was rejected, so the field typed into can say it.
 */
internal class BlockTypeEditorActions(
    private val definition: BlockTypeDefinition,
    private val onCommand: (Invocation<*>, onSuccess: () -> Unit) -> Unit,
    private val onCommandIfValid: (Invocation<*>) -> Rejection?,
) {
    private val name get() = definition.name

    private fun dispatch(invocation: Invocation<*>, onSuccess: () -> Unit = {}) = onCommand(invocation, onSuccess)

    fun delete() = when (definition) {
        is ActorTypeDefinition -> dispatch(Invocation(DeleteActorType, DeleteActorType.Input(name)))
        is ManagerTypeDefinition -> dispatch(Invocation(DeleteManagerType, DeleteManagerType.Input(name)))
    }

    fun setParent(parent: String?) = dispatch(Invocation(SetParent, SetParent.Input(name, parent)))

    fun setDescription(description: String) = dispatch(Invocation(SetDescription, SetDescription.Input(type = name, description = description)))

    fun discardChangesMadeByHand() = dispatch(Invocation(DiscardChangesMadeByHand, DiscardChangesMadeByHand.Input(name)))

    fun setAppearance(input: SetAppearance.Input) = dispatch(Invocation(SetAppearance, input))

    fun addProperty(property: PropertyDefinition) = dispatch(Invocation(AddProperty, AddProperty.Input(name, property)))

    fun rename(newName: String): Rejection? = onCommandIfValid(
        when (definition) {
            is ActorTypeDefinition -> Invocation(RenameActorType, RenameActorType.Input(name, newName))
            is ManagerTypeDefinition -> Invocation(RenameManagerType, RenameManagerType.Input(name, newName))
        },
    )

    fun editProperty(propertyName: String, property: PropertyDefinition) = dispatch(Invocation(EditProperty, EditProperty.Input(name, propertyName, property)))

    fun renameProperty(propertyName: String, property: PropertyDefinition): Rejection? = onCommandIfValid(Invocation(EditProperty, EditProperty.Input(name, propertyName, property)))

    fun removeProperty(propertyName: String) = dispatch(Invocation(RemoveProperty, RemoveProperty.Input(name, propertyName)))

    fun addText() = dispatch(Invocation(AddText, AddText.Input(name, ScreenTextDefinition.defaultFor(definition.properties))))

    fun editText(index: Int, text: ScreenTextDefinition) = dispatch(Invocation(EditText, EditText.Input(name, index, text)))

    fun removeText(index: Int) = dispatch(Invocation(RemoveText, RemoveText.Input(name, index)))

    fun setKotlinUi(code: String) = dispatch(Invocation(SetKotlinUi, SetKotlinUi.Input(name, code)))

    fun addEvent(type: EventType, onAdded: () -> Unit) = dispatch(Invocation(AddEvent, AddEvent.Input(name, type)), onAdded)

    fun overrideEvent(event: EventDefinition, onAdded: () -> Unit) = dispatch(
        Invocation(AddEvent, AddEvent.Input(name, event.type, event.key, event.otherActorType, event.timer, listOf(BlockDefinition.RunParentEvent))),
        onAdded,
    )

    fun moveEvent(index: Int, targetIndex: Int) = dispatch(Invocation(MoveEvent, MoveEvent.Input(name, index, targetIndex)))

    fun removeEvent(index: Int) = dispatch(Invocation(RemoveEvent, RemoveEvent.Input(name, index)))

    fun setEventKey(eventIndex: Int, key: KeyChoice) = dispatch(Invocation(SetEventKey, SetEventKey.Input(name, eventIndex, key)))

    fun setEventTimer(eventIndex: Int, timer: String) = dispatch(Invocation(SetEventTimer, SetEventTimer.Input(name, eventIndex, timer)))

    fun setEventOtherActorType(eventIndex: Int, other: String) = dispatch(
        Invocation(SetEventOtherActorType, SetEventOtherActorType.Input(name, eventIndex, other)),
    )

    fun renameTimer(oldName: String, newName: String) = dispatch(Invocation(RenameTimer, RenameTimer.Input(name, oldName, newName)))

    fun deleteTimer(timer: String) = dispatch(Invocation(DeleteTimer, DeleteTimer.Input(name, timer)))

    fun addBlock(eventIndex: Int, kind: BlockKind, gap: Int? = null) = dispatch(
        Invocation(AddBlock, AddBlock.Input(name, eventIndex, kind.createDefault(), index = gap)),
    )

    fun moveBlock(eventIndex: Int, index: Int, gap: Int) {
        moveTarget(index, gap)?.let { dispatch(Invocation(MoveBlock, MoveBlock.Input(name, eventIndex, index, it))) }
    }

    fun editBlock(eventIndex: Int, blockIndex: Int, block: BlockDefinition) = dispatch(Invocation(EditBlock, EditBlock.Input(name, eventIndex, blockIndex, block)))

    fun removeBlock(eventIndex: Int, blockIndex: Int) = dispatch(Invocation(RemoveBlock, RemoveBlock.Input(name, eventIndex, blockIndex)))
}
