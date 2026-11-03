/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.texts

import com.pandulapeter.kubrikoCreator.workspace.WorkspaceStatus
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_android_app_missing
import kubriko.creator.generated.resources.status_android_device_left
import kubriko.creator.generated.resources.status_android_device_not_connected
import kubriko.creator.generated.resources.status_android_device_offline
import kubriko.creator.generated.resources.status_android_device_unauthorized
import kubriko.creator.generated.resources.status_android_emulator_failed
import kubriko.creator.generated.resources.status_android_emulator_starting
import kubriko.creator.generated.resources.status_android_emulator_timed_out
import kubriko.creator.generated.resources.status_android_game_crashed
import kubriko.creator.generated.resources.status_android_game_ended_at_once
import kubriko.creator.generated.resources.status_android_game_started
import kubriko.creator.generated.resources.status_android_install_failed
import kubriko.creator.generated.resources.status_android_install_incompatible
import kubriko.creator.generated.resources.status_android_installing
import kubriko.creator.generated.resources.status_android_not_started
import kubriko.creator.generated.resources.status_android_sdk_missing
import kubriko.creator.generated.resources.status_block_type_not_saved
import kubriko.creator.generated.resources.status_block_types_saved_again
import kubriko.creator.generated.resources.status_browser_not_opened
import kubriko.creator.generated.resources.status_build_failed
import kubriko.creator.generated.resources.status_building
import kubriko.creator.generated.resources.status_building_with_unsaved_types
import kubriko.creator.generated.resources.status_building_with_unwritten_generated_code
import kubriko.creator.generated.resources.status_built
import kubriko.creator.generated.resources.status_game_changes_not_applied
import kubriko.creator.generated.resources.status_game_failed
import kubriko.creator.generated.resources.status_game_not_started
import kubriko.creator.generated.resources.status_game_reload_failed
import kubriko.creator.generated.resources.status_game_reloaded
import kubriko.creator.generated.resources.status_game_restarting
import kubriko.creator.generated.resources.status_generated_code_not_written
import kubriko.creator.generated.resources.status_generated_leftover_changed_by_hand
import kubriko.creator.generated.resources.status_generated_leftovers_removed
import kubriko.creator.generated.resources.status_kotlin_block_scope_changed
import kubriko.creator.generated.resources.status_kubriko_libraries_added
import kubriko.creator.generated.resources.status_kubriko_libraries_removed
import kubriko.creator.generated.resources.status_kubriko_version_changed
import kubriko.creator.generated.resources.status_kubriko_version_newer
import kubriko.creator.generated.resources.status_kubriko_version_unavailable
import kubriko.creator.generated.resources.status_play_from_scene_needs_upgrade
import kubriko.creator.generated.resources.status_session_ended
import kubriko.creator.generated.resources.status_session_started
import kubriko.creator.generated.resources.status_session_stopped
import kubriko.creator.generated.resources.status_type_file_name_duplicate
import kubriko.creator.generated.resources.status_type_file_name_invalid
import kubriko.creator.generated.resources.status_type_file_name_mismatch
import kubriko.creator.generated.resources.status_type_file_name_reserved
import kubriko.creator.generated.resources.status_type_file_property_name_invalid
import kubriko.creator.generated.resources.status_type_file_unreadable
import kubriko.creator.generated.resources.status_type_files_readable_again
import kubriko.creator.generated.resources.status_type_folder_not_saved
import kubriko.creator.generated.resources.status_unexpected_problem
import kubriko.creator.generated.resources.status_unsupported_protocol
import kubriko.creator.generated.resources.status_web_game_missing
import kubriko.creator.generated.resources.status_web_game_reloaded
import kubriko.creator.generated.resources.status_web_game_served
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [WorkspaceStatus].
 */
internal fun WorkspaceStatus.toResource(): StringResource = when (this) {
    WorkspaceStatus.ANDROID_APP_MISSING -> Res.string.status_android_app_missing
    WorkspaceStatus.ANDROID_DEVICE_LEFT -> Res.string.status_android_device_left
    WorkspaceStatus.ANDROID_DEVICE_NOT_CONNECTED -> Res.string.status_android_device_not_connected
    WorkspaceStatus.ANDROID_DEVICE_OFFLINE -> Res.string.status_android_device_offline
    WorkspaceStatus.ANDROID_DEVICE_UNAUTHORIZED -> Res.string.status_android_device_unauthorized
    WorkspaceStatus.ANDROID_EMULATOR_FAILED -> Res.string.status_android_emulator_failed
    WorkspaceStatus.ANDROID_EMULATOR_STARTING -> Res.string.status_android_emulator_starting
    WorkspaceStatus.ANDROID_EMULATOR_TIMED_OUT -> Res.string.status_android_emulator_timed_out
    WorkspaceStatus.ANDROID_GAME_CRASHED -> Res.string.status_android_game_crashed
    WorkspaceStatus.ANDROID_GAME_ENDED_AT_ONCE -> Res.string.status_android_game_ended_at_once
    WorkspaceStatus.ANDROID_GAME_STARTED -> Res.string.status_android_game_started
    WorkspaceStatus.ANDROID_INSTALL_FAILED -> Res.string.status_android_install_failed
    WorkspaceStatus.ANDROID_INSTALL_INCOMPATIBLE -> Res.string.status_android_install_incompatible
    WorkspaceStatus.ANDROID_INSTALLING -> Res.string.status_android_installing
    WorkspaceStatus.ANDROID_NOT_STARTED -> Res.string.status_android_not_started
    WorkspaceStatus.ANDROID_SDK_MISSING -> Res.string.status_android_sdk_missing
    WorkspaceStatus.BLOCK_TYPE_NOT_SAVED -> Res.string.status_block_type_not_saved
    WorkspaceStatus.BLOCK_TYPES_SAVED_AGAIN -> Res.string.status_block_types_saved_again
    WorkspaceStatus.BROWSER_NOT_OPENED -> Res.string.status_browser_not_opened
    WorkspaceStatus.BUILD_FAILED -> Res.string.status_build_failed
    WorkspaceStatus.BUILDING -> Res.string.status_building
    WorkspaceStatus.BUILDING_WITH_UNSAVED_TYPES -> Res.string.status_building_with_unsaved_types
    WorkspaceStatus.BUILDING_WITH_UNWRITTEN_GENERATED_CODE -> Res.string.status_building_with_unwritten_generated_code
    WorkspaceStatus.BUILT -> Res.string.status_built
    WorkspaceStatus.GAME_CHANGES_NOT_APPLIED -> Res.string.status_game_changes_not_applied
    WorkspaceStatus.GAME_FAILED -> Res.string.status_game_failed
    WorkspaceStatus.GAME_NOT_STARTED -> Res.string.status_game_not_started
    WorkspaceStatus.GAME_RELOAD_FAILED -> Res.string.status_game_reload_failed
    WorkspaceStatus.GAME_RELOADED -> Res.string.status_game_reloaded
    WorkspaceStatus.GAME_RESTARTING -> Res.string.status_game_restarting
    WorkspaceStatus.GENERATED_CODE_NOT_WRITTEN -> Res.string.status_generated_code_not_written
    WorkspaceStatus.GENERATED_LEFTOVER_CHANGED_BY_HAND -> Res.string.status_generated_leftover_changed_by_hand
    WorkspaceStatus.GENERATED_LEFTOVERS_REMOVED -> Res.string.status_generated_leftovers_removed
    WorkspaceStatus.KOTLIN_BLOCK_SCOPE_CHANGED -> Res.string.status_kotlin_block_scope_changed
    WorkspaceStatus.KUBRIKO_LIBRARIES_ADDED -> Res.string.status_kubriko_libraries_added
    WorkspaceStatus.KUBRIKO_LIBRARIES_REMOVED -> Res.string.status_kubriko_libraries_removed
    WorkspaceStatus.KUBRIKO_VERSION_CHANGED -> Res.string.status_kubriko_version_changed
    WorkspaceStatus.KUBRIKO_VERSION_NEWER -> Res.string.status_kubriko_version_newer
    WorkspaceStatus.KUBRIKO_VERSION_UNAVAILABLE -> Res.string.status_kubriko_version_unavailable
    WorkspaceStatus.PLAY_FROM_SCENE_NEEDS_UPGRADE -> Res.string.status_play_from_scene_needs_upgrade
    WorkspaceStatus.SESSION_ENDED -> Res.string.status_session_ended
    WorkspaceStatus.SESSION_STARTED -> Res.string.status_session_started
    WorkspaceStatus.SESSION_STOPPED -> Res.string.status_session_stopped
    WorkspaceStatus.TYPE_FILE_NAME_DUPLICATE -> Res.string.status_type_file_name_duplicate
    WorkspaceStatus.TYPE_FILE_NAME_INVALID -> Res.string.status_type_file_name_invalid
    WorkspaceStatus.TYPE_FILE_NAME_MISMATCH -> Res.string.status_type_file_name_mismatch
    WorkspaceStatus.TYPE_FILE_NAME_RESERVED -> Res.string.status_type_file_name_reserved
    WorkspaceStatus.TYPE_FILE_PROPERTY_NAME_INVALID -> Res.string.status_type_file_property_name_invalid
    WorkspaceStatus.TYPE_FILE_UNREADABLE -> Res.string.status_type_file_unreadable
    WorkspaceStatus.TYPE_FILES_READABLE_AGAIN -> Res.string.status_type_files_readable_again
    WorkspaceStatus.TYPE_FOLDER_NOT_SAVED -> Res.string.status_type_folder_not_saved
    WorkspaceStatus.UNEXPECTED_PROBLEM -> Res.string.status_unexpected_problem
    WorkspaceStatus.UNSUPPORTED_PROTOCOL -> Res.string.status_unsupported_protocol
    WorkspaceStatus.WEB_GAME_MISSING -> Res.string.status_web_game_missing
    WorkspaceStatus.WEB_GAME_RELOADED -> Res.string.status_web_game_reloaded
    WorkspaceStatus.WEB_GAME_SERVED -> Res.string.status_web_game_served
}
