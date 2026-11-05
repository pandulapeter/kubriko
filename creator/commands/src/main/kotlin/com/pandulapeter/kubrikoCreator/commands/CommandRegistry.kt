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

import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DiscardChangesMadeByHand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.GetBlockCatalog
import com.pandulapeter.kubrikoCreator.commands.actorTypes.ListActorTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.ReadActorType
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
import com.pandulapeter.kubrikoCreator.commands.folders.CreateFolder
import com.pandulapeter.kubrikoCreator.commands.folders.DeleteFolder
import com.pandulapeter.kubrikoCreator.commands.folders.MoveToFolder
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.commands.game.ListPlayTargets
import com.pandulapeter.kubrikoCreator.commands.game.PressKeys
import com.pandulapeter.kubrikoCreator.commands.game.StartGame
import com.pandulapeter.kubrikoCreator.commands.game.StopGame
import com.pandulapeter.kubrikoCreator.commands.game.TakeScreenshot
import com.pandulapeter.kubrikoCreator.commands.game.Wait
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.commands.git.DiscardChanges
import com.pandulapeter.kubrikoCreator.commands.git.StartVersionControl
import com.pandulapeter.kubrikoCreator.commands.managerTypes.AddText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.CreateManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.EditText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.ListManagerTypes
import com.pandulapeter.kubrikoCreator.commands.managerTypes.ReadManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RemoveText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RenameManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.SetKotlinUi
import com.pandulapeter.kubrikoCreator.commands.project.Build
import com.pandulapeter.kubrikoCreator.commands.project.CheckChange
import com.pandulapeter.kubrikoCreator.commands.project.GetHealthChecks
import com.pandulapeter.kubrikoCreator.commands.project.GetKubrikoApi
import com.pandulapeter.kubrikoCreator.commands.project.GetProjectDigest
import com.pandulapeter.kubrikoCreator.commands.project.GetProjectOverview
import com.pandulapeter.kubrikoCreator.commands.project.GetProjectSettings
import com.pandulapeter.kubrikoCreator.commands.project.GetProjectUpgrade
import com.pandulapeter.kubrikoCreator.commands.project.GetRelations
import com.pandulapeter.kubrikoCreator.commands.project.GetTargetChange
import com.pandulapeter.kubrikoCreator.commands.project.GetVersionControlStatus
import com.pandulapeter.kubrikoCreator.commands.project.Play
import com.pandulapeter.kubrikoCreator.commands.project.ReadConsole
import com.pandulapeter.kubrikoCreator.commands.project.ReadGameNotes
import com.pandulapeter.kubrikoCreator.commands.project.SetDescription
import com.pandulapeter.kubrikoCreator.commands.project.SetGameNotes
import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.commands.project.SetTarget
import com.pandulapeter.kubrikoCreator.commands.project.UpgradeProject
import com.pandulapeter.kubrikoCreator.commands.scenes.CreateScene
import com.pandulapeter.kubrikoCreator.commands.scenes.DeleteScene
import com.pandulapeter.kubrikoCreator.commands.scenes.ListScenes
import com.pandulapeter.kubrikoCreator.commands.scenes.MoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.MoveActorOrder
import com.pandulapeter.kubrikoCreator.commands.scenes.PlaceActor
import com.pandulapeter.kubrikoCreator.commands.scenes.PlaceActors
import com.pandulapeter.kubrikoCreator.commands.scenes.ReadScene
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActors
import com.pandulapeter.kubrikoCreator.commands.scenes.RenameScene
import com.pandulapeter.kubrikoCreator.commands.scenes.RestoreActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RestoreActors
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorState
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorValues
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorsState
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorsValues
import com.pandulapeter.kubrikoCreator.commands.scenes.SetSceneSettings
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddDocumentToScreenplay
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreateDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreatePage
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeleteDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeletePage
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditPage
import com.pandulapeter.kubrikoCreator.commands.screenplay.ListPages
import com.pandulapeter.kubrikoCreator.commands.screenplay.MoveDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.MovePage
import com.pandulapeter.kubrikoCreator.commands.screenplay.ReadDocuments
import com.pandulapeter.kubrikoCreator.commands.screenplay.ReadPages

/**
 * Every command of the editor. The AI tool list is generated from it, so a command reaches the AI as soon as it is
 * registered, and a test fails if a command exists that isn't.
 */
public class CommandRegistry(
    public val commands: List<EditorCommand<*>>,
) {
    private val commandsByName = commands.associateBy { it.name }

    init {
        require(commandsByName.size == commands.size) { "Command names must be unique." }
    }

    public operator fun get(name: String): EditorCommand<*>? = commandsByName[name]

    public val tools: List<ToolDefinition> by lazy {
        commands.map { command ->
            ToolDefinition(
                name = command.name,
                description = command.description,
                inputSchema = JsonSchema.of(command.inputSerializer.descriptor),
                isReadOnly = command is ReadCommand,
                isDestructive = command is WriteCommand && command.isDestructive,
            )
        }
    }

    public companion object {
        public val editor: CommandRegistry = CommandRegistry(
            commands = listOf(
                GetProjectDigest,
                GetProjectOverview,
                GetBlockCatalog,
                ListActorTypes,
                ReadActorType,
                ListManagerTypes,
                ReadManagerType,
                ListScenes,
                ReadScene,
                GetRelations,
                GetHealthChecks,
                CheckChange,
                ReadGameNotes,
                ListPages,
                ReadPages,
                ReadDocuments,
                GetKubrikoApi,
                ReadConsole,
                GetVersionControlStatus,
                GetProjectUpgrade,
                GetProjectSettings,
                GetTargetChange,
                Build,
                Play,
                ListPlayTargets,
                StartGame,
                PressKeys,
                Wait,
                TakeScreenshot,
                StopGame,
                CreateActorType,
                RenameActorType,
                DeleteActorType,
                SetAppearance,
                SetParent,
                SetDescription,
                SetGameNotes,
                CreatePage,
                EditPage,
                MovePage,
                DeletePage,
                CreateDocument,
                EditDocument,
                MoveDocument,
                DeleteDocument,
                AddDocumentToScreenplay,
                SetProjectSettings,
                SetTarget,
                CreateManagerType,
                RenameManagerType,
                DeleteManagerType,
                AddText,
                EditText,
                RemoveText,
                SetKotlinUi,
                AddEvent,
                RemoveEvent,
                MoveEvent,
                SetEventKey,
                SetEventTimer,
                RenameTimer,
                SetEventOtherActorType,
                AddBlock,
                EditBlock,
                MoveBlock,
                RemoveBlock,
                AddProperty,
                EditProperty,
                RemoveProperty,
                DiscardChangesMadeByHand,
                CreateFolder,
                RenameFolder,
                DeleteFolder,
                MoveToFolder,
                CreateScene,
                DeleteScene,
                RenameScene,
                PlaceActor,
                PlaceActors,
                MoveActor,
                MoveActorOrder,
                SetActorValues,
                SetActorState,
                SetActorsValues,
                SetActorsState,
                SetSceneSettings,
                RemoveActor,
                RemoveActors,
                RestoreActor,
                RestoreActors,
                StartVersionControl,
                Commit,
                DiscardChanges,
                UpgradeProject,
            ),
        )
    }
}
