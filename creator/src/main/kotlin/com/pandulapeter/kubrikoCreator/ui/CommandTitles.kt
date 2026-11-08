/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
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
import com.pandulapeter.kubrikoCreator.commands.project.AskUser
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
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddPageImage
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
import com.pandulapeter.kubrikoCreator.commands.screenplay.SetScreenplayReminders
import com.pandulapeter.kubrikoCreator.commands.screenplay.ViewPageImage
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_add_block
import kubriko.creator.generated.resources.command_add_document_to_screenplay
import kubriko.creator.generated.resources.command_add_event
import kubriko.creator.generated.resources.command_add_page_image
import kubriko.creator.generated.resources.command_add_property
import kubriko.creator.generated.resources.command_add_text
import kubriko.creator.generated.resources.command_ask_user
import kubriko.creator.generated.resources.command_build
import kubriko.creator.generated.resources.command_check_change
import kubriko.creator.generated.resources.command_commit
import kubriko.creator.generated.resources.command_create_actor_type
import kubriko.creator.generated.resources.command_create_document
import kubriko.creator.generated.resources.command_create_folder
import kubriko.creator.generated.resources.command_create_manager_type
import kubriko.creator.generated.resources.command_create_page
import kubriko.creator.generated.resources.command_create_scene
import kubriko.creator.generated.resources.command_delete_actor_type
import kubriko.creator.generated.resources.command_delete_document
import kubriko.creator.generated.resources.command_delete_folder
import kubriko.creator.generated.resources.command_delete_manager_type
import kubriko.creator.generated.resources.command_delete_page
import kubriko.creator.generated.resources.command_delete_scene
import kubriko.creator.generated.resources.command_discard_changes
import kubriko.creator.generated.resources.command_discard_changes_made_by_hand
import kubriko.creator.generated.resources.command_edit_block
import kubriko.creator.generated.resources.command_edit_document
import kubriko.creator.generated.resources.command_edit_page
import kubriko.creator.generated.resources.command_edit_property
import kubriko.creator.generated.resources.command_edit_text
import kubriko.creator.generated.resources.command_get_block_catalog
import kubriko.creator.generated.resources.command_get_health_checks
import kubriko.creator.generated.resources.command_get_kubriko_api
import kubriko.creator.generated.resources.command_get_project_digest
import kubriko.creator.generated.resources.command_get_project_overview
import kubriko.creator.generated.resources.command_get_project_settings
import kubriko.creator.generated.resources.command_get_project_upgrade
import kubriko.creator.generated.resources.command_get_relations
import kubriko.creator.generated.resources.command_get_target_change
import kubriko.creator.generated.resources.command_get_version_control_status
import kubriko.creator.generated.resources.command_list_actor_types
import kubriko.creator.generated.resources.command_list_manager_types
import kubriko.creator.generated.resources.command_list_pages
import kubriko.creator.generated.resources.command_list_play_targets
import kubriko.creator.generated.resources.command_list_scenes
import kubriko.creator.generated.resources.command_move_actor
import kubriko.creator.generated.resources.command_move_actor_order
import kubriko.creator.generated.resources.command_move_block
import kubriko.creator.generated.resources.command_move_document
import kubriko.creator.generated.resources.command_move_event
import kubriko.creator.generated.resources.command_move_page
import kubriko.creator.generated.resources.command_move_to_folder
import kubriko.creator.generated.resources.command_place_actor
import kubriko.creator.generated.resources.command_place_actors
import kubriko.creator.generated.resources.command_play
import kubriko.creator.generated.resources.command_press_keys
import kubriko.creator.generated.resources.command_read_actor_type
import kubriko.creator.generated.resources.command_read_console
import kubriko.creator.generated.resources.command_read_documents
import kubriko.creator.generated.resources.command_read_game_notes
import kubriko.creator.generated.resources.command_read_manager_type
import kubriko.creator.generated.resources.command_read_pages
import kubriko.creator.generated.resources.command_read_scene
import kubriko.creator.generated.resources.command_remove_actor
import kubriko.creator.generated.resources.command_remove_actors
import kubriko.creator.generated.resources.command_remove_block
import kubriko.creator.generated.resources.command_remove_event
import kubriko.creator.generated.resources.command_remove_property
import kubriko.creator.generated.resources.command_remove_text
import kubriko.creator.generated.resources.command_rename_actor_type
import kubriko.creator.generated.resources.command_rename_folder
import kubriko.creator.generated.resources.command_rename_manager_type
import kubriko.creator.generated.resources.command_rename_scene
import kubriko.creator.generated.resources.command_rename_timer
import kubriko.creator.generated.resources.command_restore_actor
import kubriko.creator.generated.resources.command_restore_actors
import kubriko.creator.generated.resources.command_set_actor_state
import kubriko.creator.generated.resources.command_set_actor_values
import kubriko.creator.generated.resources.command_set_actors_state
import kubriko.creator.generated.resources.command_set_actors_values
import kubriko.creator.generated.resources.command_set_appearance
import kubriko.creator.generated.resources.command_set_description
import kubriko.creator.generated.resources.command_set_event_key
import kubriko.creator.generated.resources.command_set_event_other_actor_type
import kubriko.creator.generated.resources.command_set_event_timer
import kubriko.creator.generated.resources.command_set_game_notes
import kubriko.creator.generated.resources.command_set_kotlin_ui
import kubriko.creator.generated.resources.command_set_parent
import kubriko.creator.generated.resources.command_set_project_settings
import kubriko.creator.generated.resources.command_set_scene_settings
import kubriko.creator.generated.resources.command_set_screenplay_reminders
import kubriko.creator.generated.resources.command_set_target
import kubriko.creator.generated.resources.command_start_game
import kubriko.creator.generated.resources.command_start_version_control
import kubriko.creator.generated.resources.command_stop_game
import kubriko.creator.generated.resources.command_take_screenshot
import kubriko.creator.generated.resources.command_upgrade_project
import kubriko.creator.generated.resources.command_view_page_image
import kubriko.creator.generated.resources.command_wait
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * How the user interface names each command (undo and redo, the AI activity bar, a turn's changes), by its tool name.
 */
internal fun commandTitle(name: String): StringResource? = commandTitles[name]

/**
 * The text of [commandTitle] for the command called [name], or the name itself for one without a title.
 */
@Composable
internal fun commandTitleText(name: String) = commandTitle(name)?.let { stringResource(it) } ?: name

/**
 * The tool names [commandTitle] knows.
 */
internal val commandTitleNames: Set<String> get() = commandTitles.keys

private val commandTitles = mapOf(
    AddBlock.name to Res.string.command_add_block,
    AddEvent.name to Res.string.command_add_event,
    AddProperty.name to Res.string.command_add_property,
    CreateActorType.name to Res.string.command_create_actor_type,
    DeleteActorType.name to Res.string.command_delete_actor_type,
    DiscardChangesMadeByHand.name to Res.string.command_discard_changes_made_by_hand,
    EditBlock.name to Res.string.command_edit_block,
    EditProperty.name to Res.string.command_edit_property,
    GetBlockCatalog.name to Res.string.command_get_block_catalog,
    ListActorTypes.name to Res.string.command_list_actor_types,
    MoveBlock.name to Res.string.command_move_block,
    MoveEvent.name to Res.string.command_move_event,
    ReadActorType.name to Res.string.command_read_actor_type,
    RemoveBlock.name to Res.string.command_remove_block,
    RemoveEvent.name to Res.string.command_remove_event,
    RemoveProperty.name to Res.string.command_remove_property,
    RenameActorType.name to Res.string.command_rename_actor_type,
    RenameTimer.name to Res.string.command_rename_timer,
    SetAppearance.name to Res.string.command_set_appearance,
    SetEventKey.name to Res.string.command_set_event_key,
    SetEventOtherActorType.name to Res.string.command_set_event_other_actor_type,
    SetEventTimer.name to Res.string.command_set_event_timer,
    SetParent.name to Res.string.command_set_parent,
    CreateFolder.name to Res.string.command_create_folder,
    DeleteFolder.name to Res.string.command_delete_folder,
    MoveToFolder.name to Res.string.command_move_to_folder,
    RenameFolder.name to Res.string.command_rename_folder,
    PressKeys.name to Res.string.command_press_keys,
    ListPlayTargets.name to Res.string.command_list_play_targets,
    StartGame.name to Res.string.command_start_game,
    StopGame.name to Res.string.command_stop_game,
    TakeScreenshot.name to Res.string.command_take_screenshot,
    Wait.name to Res.string.command_wait,
    Commit.name to Res.string.command_commit,
    DiscardChanges.name to Res.string.command_discard_changes,
    StartVersionControl.name to Res.string.command_start_version_control,
    AddText.name to Res.string.command_add_text,
    CreateManagerType.name to Res.string.command_create_manager_type,
    DeleteManagerType.name to Res.string.command_delete_manager_type,
    EditText.name to Res.string.command_edit_text,
    ListManagerTypes.name to Res.string.command_list_manager_types,
    ReadManagerType.name to Res.string.command_read_manager_type,
    RemoveText.name to Res.string.command_remove_text,
    RenameManagerType.name to Res.string.command_rename_manager_type,
    SetKotlinUi.name to Res.string.command_set_kotlin_ui,
    Build.name to Res.string.command_build,
    CheckChange.name to Res.string.command_check_change,
    GetHealthChecks.name to Res.string.command_get_health_checks,
    GetKubrikoApi.name to Res.string.command_get_kubriko_api,
    GetProjectDigest.name to Res.string.command_get_project_digest,
    GetProjectOverview.name to Res.string.command_get_project_overview,
    GetProjectSettings.name to Res.string.command_get_project_settings,
    GetProjectUpgrade.name to Res.string.command_get_project_upgrade,
    GetTargetChange.name to Res.string.command_get_target_change,
    GetRelations.name to Res.string.command_get_relations,
    GetVersionControlStatus.name to Res.string.command_get_version_control_status,
    Play.name to Res.string.command_play,
    ReadConsole.name to Res.string.command_read_console,
    ReadGameNotes.name to Res.string.command_read_game_notes,
    SetDescription.name to Res.string.command_set_description,
    SetGameNotes.name to Res.string.command_set_game_notes,
    ListPages.name to Res.string.command_list_pages,
    ReadPages.name to Res.string.command_read_pages,
    ViewPageImage.name to Res.string.command_view_page_image,
    CreatePage.name to Res.string.command_create_page,
    EditPage.name to Res.string.command_edit_page,
    MovePage.name to Res.string.command_move_page,
    AddPageImage.name to Res.string.command_add_page_image,
    DeletePage.name to Res.string.command_delete_page,
    ReadDocuments.name to Res.string.command_read_documents,
    CreateDocument.name to Res.string.command_create_document,
    EditDocument.name to Res.string.command_edit_document,
    MoveDocument.name to Res.string.command_move_document,
    DeleteDocument.name to Res.string.command_delete_document,
    AddDocumentToScreenplay.name to Res.string.command_add_document_to_screenplay,
    SetScreenplayReminders.name to Res.string.command_set_screenplay_reminders,
    AskUser.name to Res.string.command_ask_user,
    SetProjectSettings.name to Res.string.command_set_project_settings,
    SetTarget.name to Res.string.command_set_target,
    UpgradeProject.name to Res.string.command_upgrade_project,
    CreateScene.name to Res.string.command_create_scene,
    DeleteScene.name to Res.string.command_delete_scene,
    ListScenes.name to Res.string.command_list_scenes,
    MoveActor.name to Res.string.command_move_actor,
    MoveActorOrder.name to Res.string.command_move_actor_order,
    PlaceActor.name to Res.string.command_place_actor,
    PlaceActors.name to Res.string.command_place_actors,
    ReadScene.name to Res.string.command_read_scene,
    RemoveActor.name to Res.string.command_remove_actor,
    RemoveActors.name to Res.string.command_remove_actors,
    RenameScene.name to Res.string.command_rename_scene,
    RestoreActor.name to Res.string.command_restore_actor,
    RestoreActors.name to Res.string.command_restore_actors,
    SetActorState.name to Res.string.command_set_actor_state,
    SetActorValues.name to Res.string.command_set_actor_values,
    SetActorsState.name to Res.string.command_set_actors_state,
    SetActorsValues.name to Res.string.command_set_actors_values,
    SetSceneSettings.name to Res.string.command_set_scene_settings,
)
