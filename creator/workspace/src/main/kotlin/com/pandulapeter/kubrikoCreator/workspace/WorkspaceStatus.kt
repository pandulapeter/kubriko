/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * What an open project reports in the console: builds, sessions, type files, and upgrades.
 */
public enum class WorkspaceStatus(override val english: String) : DomainText {
    ANDROID_APP_MISSING("The Android build finished, but there's no app in %1\$s to install."),
    ANDROID_DEVICE_LEFT("%1\$s was disconnected, so the game ended."),
    ANDROID_DEVICE_NOT_CONNECTED("%1\$s isn't connected, so the game can't be played on it. Connect it, or choose another place in Play's menu."),
    ANDROID_DEVICE_OFFLINE("%1\$s doesn't answer (it's %2\$s), so the game can't be played on it. Connect it again, then play again."),
    ANDROID_DEVICE_UNAUTHORIZED("%1\$s doesn't allow this computer to debug it yet. Unlock it, allow USB debugging when it asks, then play again."),
    ANDROID_EMULATOR_FAILED("The emulator %1\$s stopped before it was ready. The last lines it printed:"),
    ANDROID_EMULATOR_STARTING("Starting the emulator %1\$s, which can take a minute…"),
    ANDROID_EMULATOR_TIMED_OUT("The emulator %1\$s wasn't ready within %2\$s minutes, so the game wasn't played on it."),
    ANDROID_GAME_CRASHED("The game stopped with an error on %1\$s."),
    ANDROID_GAME_STARTED("The game plays on %1\$s. Its logs and errors show here."),
    ANDROID_INSTALL_FAILED("The game couldn't be installed on %1\$s: %2\$s"),
    ANDROID_INSTALL_INCOMPATIBLE("The game couldn't be installed on %1\$s, since a build of it signed on another computer is there. Uninstall that one from the device, then play again."),
    ANDROID_INSTALLING("Installing the game on %1\$s…"),
    ANDROID_NOT_STARTED("The game couldn't be started on %1\$s: %2\$s"),
    ANDROID_SDK_MISSING("There's no Android SDK on this computer, so the game can't be played on Android."),
    BLOCK_TYPE_NOT_SAVED("%1\$s couldn't be saved: %2\$s"),
    BLOCK_TYPES_SAVED_AGAIN("Kubriko Creator could write its changes again."),
    BROWSER_NOT_OPENED("The browser couldn't be opened, so open %1\$s in one to play."),
    BUILD_FAILED("The build failed."),
    BUILDING("Building the project…"),
    BUILDING_WITH_UNSAVED_TYPES("Building with %1\$s as it is on disk, since Kubriko Creator couldn't save it: %2\$s"),
    BUILDING_WITH_UNWRITTEN_GENERATED_CODE("Building with the generated code as it is on disk, since Kubriko Creator couldn't write it: %1\$s"),
    BUILT("Built in %1\$s seconds."),
    GAME_CHANGES_NOT_APPLIED("The changes couldn't be applied to the running game, and the next change tries again: %1\$s"),
    GAME_FAILED("The game stopped with an error (exit code %1\$s). The last lines it printed:"),
    GAME_NOT_STARTED("The game couldn't be started: %1\$s"),
    GAME_RELOAD_FAILED("The changes couldn't be applied to the running game, so it starts again."),
    GAME_RELOADED("Changes applied to the running game in %1\$s seconds."),
    GAME_RESTARTING("The game starts again, since %1\$s changed in a way the running game can't take in, such as a new Property."),
    GENERATED_CODE_NOT_WRITTEN("The generated code couldn't be written: %1\$s"),
    GENERATED_LEFTOVER_CHANGED_BY_HAND("%1\$s is the generated code of a type that no longer exists, but it was changed by hand, so Kubriko Creator kept it. Delete it once nothing needs it, since it may break the build."),
    GENERATED_LEFTOVERS_REMOVED("Deleted the generated code of types that no longer exist: %1\$s"),
    KOTLIN_BLOCK_SCOPE_CHANGED("Kotlin Blocks of %1\$s use return, or a name their Event no longer has, which work differently now that each kind of Event runs in a function of its own. The Game map lists them."),
    KUBRIKO_LIBRARIES_ADDED("The project now depends on %1\$s, which its code uses."),
    KUBRIKO_LIBRARIES_REMOVED("The project no longer depends on %1\$s, which nothing uses."),
    KUBRIKO_VERSION_CHANGED("The project now uses Kubriko %1\$s, the version of this Kubriko Creator, instead of %2\$s."),
    KUBRIKO_VERSION_UNAVAILABLE(
        "Kubriko %1\$s, the version of this Kubriko Creator, can't be downloaded, so the project stays on Kubriko %2\$s. A Kubriko Creator " +
            "run from its repository needs ./gradlew :creator:run, which publishes the libraries to Maven Local first.",
    ),
    KUBRIKO_VERSION_NEWER("The project uses Kubriko %1\$s, which is newer than this Kubriko Creator, so its Scenes can't be edited until Kubriko Creator is updated."),
    PLAY_FROM_SCENE_NEEDS_UPGRADE("This project's files are older than playing from a Scene needs, so the game may start with its first Scene instead of %1\$s. Upgrading the project fixes that."),
    SESSION_ENDED("Session ended."),
    SESSION_STARTED("Session started."),
    SESSION_STOPPED("Session stopped."),
    TYPE_FILE_NAME_DUPLICATE("%1\$s holds a type of the same name as %2\$s, so those types are left out and no code is generated until all but one are renamed or removed."),
    TYPE_FILE_NAME_INVALID("%1\$s holds a type named %2\$s, which can't be a type's name (it starts with an uppercase letter and uses only letters and numbers), so that type is left out and no code is generated until it is renamed."),
    TYPE_FILE_NAME_MISMATCH("%1\$s holds a type named %2\$s rather than after the file, so that type is left out and no code is generated until the two names match."),
    TYPE_FILE_NAME_RESERVED("%1\$s holds a type named %2\$s, a name Kubriko itself uses, so that type is left out and no code is generated until it is renamed."),
    TYPE_FILE_PROPERTY_NAME_INVALID("%1\$s has a Property named %2\$s, which can't be a Property's name (it has to start with a lowercase letter, use only letters and numbers, not be a name Kotlin or the generated code uses, and differ from the type's other Properties by more than case), so that type is left out and no code is generated until it is renamed."),
    TYPE_FILE_UNREADABLE("Kubriko Creator couldn't read %1\$s, so that type is left out and no code is generated until the file is fixed: %2\$s"),
    TYPE_FILES_READABLE_AGAIN("Every type file can be read again, so the generated code is up to date again."),
    TYPE_FOLDER_NOT_SAVED("The folder %1\$s couldn't be changed: %2\$s"),
    UNEXPECTED_PROBLEM("Kubriko Creator ran into a problem: %1\$s"),
    UNSUPPORTED_PROTOCOL("This version of Kubriko Creator doesn't support the project's Kubriko version."),
    WEB_GAME_MISSING("The web build finished, but there's no game at %1\$s to play."),
    WEB_GAME_RELOADED("Built again, so the game's page loads again."),
    WEB_GAME_SERVED("The game plays in the browser at %1\$s. Its logs are in the browser's console, in its developer tools."),
}
