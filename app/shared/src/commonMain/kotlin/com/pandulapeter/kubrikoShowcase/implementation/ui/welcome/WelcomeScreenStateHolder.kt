/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kubriko.app.shared.generated.resources.Res
import kubriko.app.shared.generated.resources.ic_discord
import kubriko.app.shared.generated.resources.ic_documentation
import kubriko.app.shared.generated.resources.ic_getting_started
import kubriko.app.shared.generated.resources.ic_github
import kubriko.app.shared.generated.resources.welcome_app_details
import kubriko.app.shared.generated.resources.welcome_app_details_call_to_action_collapsed
import kubriko.app.shared.generated.resources.welcome_app_details_call_to_action_expanded
import kubriko.app.shared.generated.resources.welcome_community
import kubriko.app.shared.generated.resources.welcome_disclaimer_obfuscation
import kubriko.app.shared.generated.resources.welcome_disclaimer_web_general
import kubriko.app.shared.generated.resources.welcome_disclaimer_web_ipad
import kubriko.app.shared.generated.resources.welcome_disclaimer_web_iphone
import kubriko.app.shared.generated.resources.welcome_disclaimer_web_not_chrome_or_firefox
import kubriko.app.shared.generated.resources.welcome_documentation
import kubriko.app.shared.generated.resources.welcome_engine_details
import kubriko.app.shared.generated.resources.welcome_getting_started
import kubriko.app.shared.generated.resources.welcome_hide_details
import kubriko.app.shared.generated.resources.welcome_learning_1
import kubriko.app.shared.generated.resources.welcome_learning_2
import kubriko.app.shared.generated.resources.welcome_license
import kubriko.app.shared.generated.resources.welcome_message
import kubriko.app.shared.generated.resources.welcome_more_details
import kubriko.app.shared.generated.resources.welcome_repository

internal sealed interface WelcomeScreenStateHolder : StateHolder {

    companion object {
        val shouldShowMoreInfo = mutableStateOf(false)

        @Composable
        fun areResourcesLoaded() = areIconResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_github).value != null
                && preloadedImageVector(Res.drawable.ic_discord).value != null
                && preloadedImageVector(Res.drawable.ic_documentation).value != null
                && preloadedImageVector(Res.drawable.ic_getting_started).value != null

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.welcome_message).value.isNotBlank()
                && preloadedString(Res.string.welcome_more_details).value.isNotBlank()
                && preloadedString(Res.string.welcome_hide_details).value.isNotBlank()
                && preloadedString(Res.string.welcome_engine_details).value.isNotBlank()
                && preloadedString(Res.string.welcome_repository).value.isNotBlank()
                && preloadedString(Res.string.welcome_learning_1).value.isNotBlank()
                && preloadedString(Res.string.welcome_getting_started).value.isNotBlank()
                && preloadedString(Res.string.welcome_documentation).value.isNotBlank()
                && preloadedString(Res.string.welcome_learning_2).value.isNotBlank()
                && preloadedString(Res.string.welcome_community).value.isNotBlank()
                && preloadedString(Res.string.welcome_license).value.isNotBlank()
                && preloadedString(Res.string.welcome_app_details).value.isNotBlank()
                && preloadedString(Res.string.welcome_app_details_call_to_action_collapsed).value.isNotBlank()
                && preloadedString(Res.string.welcome_app_details_call_to_action_expanded).value.isNotBlank()
                && preloadedString(Res.string.welcome_disclaimer_obfuscation).value.isNotBlank()
                && preloadedString(Res.string.welcome_disclaimer_web_general).value.isNotBlank()
                && preloadedString(Res.string.welcome_disclaimer_web_iphone).value.isNotBlank()
                && preloadedString(Res.string.welcome_disclaimer_web_ipad).value.isNotBlank()
                && preloadedString(Res.string.welcome_disclaimer_web_not_chrome_or_firefox).value.isNotBlank()
    }
}
