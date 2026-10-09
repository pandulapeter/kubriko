/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui.about

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import com.pandulapeter.kubrikoShowcase.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kubriko.app.shared.generated.resources.Res
import kubriko.app.shared.generated.resources.ic_bug
import kubriko.app.shared.generated.resources.ic_contact
import kubriko.app.shared.generated.resources.ic_privacy_policy
import kubriko.app.shared.generated.resources.ic_review
import kubriko.app.shared.generated.resources.ic_share
import kubriko.app.shared.generated.resources.ic_website
import kubriko.app.shared.generated.resources.other_about_contact_me
import kubriko.app.shared.generated.resources.other_about_content
import kubriko.app.shared.generated.resources.other_about_content_creator
import kubriko.app.shared.generated.resources.other_about_content_footer
import kubriko.app.shared.generated.resources.other_about_content_footer_android
import kubriko.app.shared.generated.resources.other_about_content_footer_ios
import kubriko.app.shared.generated.resources.other_about_content_footer_linux
import kubriko.app.shared.generated.resources.other_about_content_footer_mac_os
import kubriko.app.shared.generated.resources.other_about_content_footer_web
import kubriko.app.shared.generated.resources.other_about_content_footer_windows
import kubriko.app.shared.generated.resources.other_about_content_license
import kubriko.app.shared.generated.resources.other_about_privacy_policy
import kubriko.app.shared.generated.resources.other_about_report_an_issue
import kubriko.app.shared.generated.resources.other_about_repository
import kubriko.app.shared.generated.resources.other_about_spread_the_word
import kubriko.app.shared.generated.resources.other_about_visit_my_website
import kubriko.app.shared.generated.resources.other_about_write_a_review

fun createAboutScreenStateHolder(): AboutScreenStateHolder = AboutScreenStateHolderImpl()

sealed interface AboutScreenStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areIconResourcesLoaded() && areStringResourcesLoaded()

        @Composable
        private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_bug).value != null
                && preloadedImageVector(Res.drawable.ic_contact).value != null
                && preloadedImageVector(Res.drawable.ic_privacy_policy).value != null
                && preloadedImageVector(Res.drawable.ic_review).value != null
                && preloadedImageVector(Res.drawable.ic_share).value != null
                && preloadedImageVector(Res.drawable.ic_website).value != null

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.other_about_content).value.isNotBlank()
                && preloadedString(Res.string.other_about_repository).value.isNotBlank()
                && preloadedString(Res.string.other_about_report_an_issue).value.isNotBlank()
                && preloadedString(Res.string.other_about_spread_the_word).value.isNotBlank()
                && preloadedString(Res.string.other_about_write_a_review).value.isNotBlank()
                && preloadedString(Res.string.other_about_contact_me).value.isNotBlank()
                && preloadedString(Res.string.other_about_visit_my_website).value.isNotBlank()
                && preloadedString(Res.string.other_about_privacy_policy).value.isNotBlank()
                && preloadedString(Res.string.other_about_content_creator).value.isNotBlank()
                && preloadedString(Res.string.other_about_content_license).value.isNotBlank()
                && preloadedString(Res.string.other_about_content_footer).value.isNotBlank()
                && preloadedString(Res.string.other_about_content_footer_android).value.isNotBlank()
                && preloadedString(Res.string.other_about_content_footer_ios).value.isNotBlank()
                && preloadedString(Res.string.other_about_content_footer_linux).value.isNotBlank()
                && preloadedString(Res.string.other_about_content_footer_mac_os).value.isNotBlank()
                && preloadedString(Res.string.other_about_content_footer_windows).value.isNotBlank()
                && preloadedString(Res.string.other_about_content_footer_web).value.isNotBlank()
    }
}

internal class AboutScreenStateHolderImpl : AboutScreenStateHolder {
    override val kubriko: Flow<Kubriko?> = emptyFlow()
    val appVersion = BuildConfig.APP_VERSION
    val libraryVersion = BuildConfig.LIBRARY_VERSION
    val platform = MetadataManager.newInstance().platform

    override fun dispose() = Unit
}
