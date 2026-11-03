/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a game's target can't be turned on or off, or why one that is on can't be built here.
 */
public enum class TargetProblem(override val english: String) : DomainText {
    ALREADY_OFF("The game isn't built for %1\$s."),
    ALREADY_ON("The game is built for %1\$s already."),
    ANDROID_SDK_MISSING("Android needs the Android SDK, which isn't on this computer. Install it (Android Studio does), or set ANDROID_HOME."),
    ANDROID_SDK_MISSING_WHILE_ON("The game is built for Android, but the Android SDK isn't on this computer, so the project can't build. Install it, or turn Android off."),
    ANDROID_SDK_DIRECTORY_INVALID("local.properties names %1\$s as the Android SDK, which it isn't, so the project can't build. Fix sdk.dir there, or remove it."),
    APPLICATION_ID_NOT_FOR_IOS("iOS bundle IDs can't hold underscores, which \"%1\$s\" has. Change the application ID first."),
    CHANGED_BY_HAND("The build files don't look like the template's anymore, so %1\$s has to be turned on or off in them by hand."),
    CHANGED_SINCE_SHOWN("The build files changed since this change to %1\$s was shown, so nothing was changed. Look at it again."),
    CHANGED_WHILE_APPLYING("A build file changed while %1\$s was being turned on or off, so nothing was changed. Try again."),
    DESKTOP_ALWAYS_ON("Kubriko Creator plays and edits the game on the desktop, so it is always built for it."),
    TEMPLATE_OUTDATED("The project's template files need an upgrade first (see get_project_upgrade)."),
    XCODE_MISSING("iOS needs a Mac with Xcode, which this computer doesn't have."),
}
