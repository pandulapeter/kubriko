/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageBitmap
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import com.pandulapeter.kubrikoShowcase.implementation.ui.welcome.WelcomeScreenStateHolder
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.back
import kubriko.app.generated.resources.debug_menu
import kubriko.app.generated.resources.demo_content_shaders
import kubriko.app.generated.resources.demo_content_shaders_subtitle
import kubriko.app.generated.resources.demo_particles
import kubriko.app.generated.resources.demo_particles_subtitle
import kubriko.app.generated.resources.demo_performance
import kubriko.app.generated.resources.demo_performance_subtitle
import kubriko.app.generated.resources.demo_physics
import kubriko.app.generated.resources.demo_physics_subtitle
import kubriko.app.generated.resources.demo_shader_animations
import kubriko.app.generated.resources.demo_shader_animations_subtitle
import kubriko.app.generated.resources.demos
import kubriko.app.generated.resources.game_annoyed_penguins
import kubriko.app.generated.resources.game_annoyed_penguins_subtitle
import kubriko.app.generated.resources.game_blockys_journey
import kubriko.app.generated.resources.game_blockys_journey_subtitle
import kubriko.app.generated.resources.game_space_squadron
import kubriko.app.generated.resources.game_space_squadron_subtitle
import kubriko.app.generated.resources.game_wallbreaker
import kubriko.app.generated.resources.game_wallbreaker_subtitle
import kubriko.app.generated.resources.games
import kubriko.app.generated.resources.ic_back
import kubriko.app.generated.resources.ic_collapse
import kubriko.app.generated.resources.ic_debug_off
import kubriko.app.generated.resources.ic_debug_on
import kubriko.app.generated.resources.ic_demos
import kubriko.app.generated.resources.ic_expand
import kubriko.app.generated.resources.ic_games
import kubriko.app.generated.resources.ic_info_off
import kubriko.app.generated.resources.ic_info_on
import kubriko.app.generated.resources.ic_other
import kubriko.app.generated.resources.ic_tests
import kubriko.app.generated.resources.img_logo
import kubriko.app.generated.resources.info
import kubriko.app.generated.resources.kubriko_showcase
import kubriko.app.generated.resources.other
import kubriko.app.generated.resources.other_about
import kubriko.app.generated.resources.other_about_subtitle
import kubriko.app.generated.resources.other_licenses
import kubriko.app.generated.resources.other_licenses_subtitle
import kubriko.app.generated.resources.test_audio
import kubriko.app.generated.resources.test_audio_subtitle
import kubriko.app.generated.resources.test_input
import kubriko.app.generated.resources.test_input_subtitle
import kubriko.app.generated.resources.tests
import kubriko.app.generated.resources.welcome
import kubriko.app.generated.resources.welcome_subtitle

internal object ResourceLoader {

    @Composable
    fun areResourcesLoaded() = WelcomeScreenStateHolder.areResourcesLoaded()
            && areIconResourcesLoaded()
            && areImageResourcesLoaded()
            && areStringResourcesLoaded()

    @Composable
    private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_back).value != null
            && preloadedImageVector(Res.drawable.ic_collapse).value != null
            && preloadedImageVector(Res.drawable.ic_debug_off).value != null
            && preloadedImageVector(Res.drawable.ic_debug_on).value != null
            && preloadedImageVector(Res.drawable.ic_demos).value != null
            && preloadedImageVector(Res.drawable.ic_expand).value != null
            && preloadedImageVector(Res.drawable.ic_games).value != null
            && preloadedImageVector(Res.drawable.ic_info_off).value != null
            && preloadedImageVector(Res.drawable.ic_info_on).value != null
            && preloadedImageVector(Res.drawable.ic_other).value != null
            && preloadedImageVector(Res.drawable.ic_tests).value != null

    @Composable
    private fun areImageResourcesLoaded() = preloadedImageBitmap(Res.drawable.img_logo).value != null

    @Composable
    private fun areStringResourcesLoaded() = preloadedString(Res.string.kubriko_showcase).value.isNotBlank()
            && preloadedString(Res.string.back).value.isNotBlank()
            && preloadedString(Res.string.demos).value.isNotBlank()
            && preloadedString(Res.string.games).value.isNotBlank()
            && preloadedString(Res.string.tests).value.isNotBlank()
            && preloadedString(Res.string.other).value.isNotBlank()
            && preloadedString(Res.string.info).value.isNotBlank()
            && preloadedString(Res.string.debug_menu).value.isNotBlank()
            && preloadedString(Res.string.welcome).value.isNotBlank()
            && preloadedString(Res.string.welcome_subtitle).value.isNotBlank()
            && preloadedString(Res.string.game_wallbreaker).value.isNotBlank()
            && preloadedString(Res.string.game_wallbreaker_subtitle).value.isNotBlank()
            && preloadedString(Res.string.game_space_squadron).value.isNotBlank()
            && preloadedString(Res.string.game_space_squadron_subtitle).value.isNotBlank()
            && preloadedString(Res.string.game_annoyed_penguins).value.isNotBlank()
            && preloadedString(Res.string.game_annoyed_penguins_subtitle).value.isNotBlank()
            && preloadedString(Res.string.game_blockys_journey).value.isNotBlank()
            && preloadedString(Res.string.game_blockys_journey_subtitle).value.isNotBlank()
            && preloadedString(Res.string.demo_content_shaders).value.isNotBlank()
            && preloadedString(Res.string.demo_content_shaders_subtitle).value.isNotBlank()
            && preloadedString(Res.string.demo_particles).value.isNotBlank()
            && preloadedString(Res.string.demo_particles_subtitle).value.isNotBlank()
            && preloadedString(Res.string.demo_performance).value.isNotBlank()
            && preloadedString(Res.string.demo_performance_subtitle).value.isNotBlank()
            && preloadedString(Res.string.demo_physics).value.isNotBlank()
            && preloadedString(Res.string.demo_physics_subtitle).value.isNotBlank()
            && preloadedString(Res.string.demo_shader_animations).value.isNotBlank()
            && preloadedString(Res.string.demo_shader_animations_subtitle).value.isNotBlank()
            && preloadedString(Res.string.test_audio).value.isNotBlank()
            && preloadedString(Res.string.test_audio_subtitle).value.isNotBlank()
            && preloadedString(Res.string.test_input).value.isNotBlank()
            && preloadedString(Res.string.test_input_subtitle).value.isNotBlank()
            && preloadedString(Res.string.other_licenses).value.isNotBlank()
            && preloadedString(Res.string.other_licenses_subtitle).value.isNotBlank()
            && preloadedString(Res.string.other_about).value.isNotBlank()
            && preloadedString(Res.string.other_about_subtitle).value.isNotBlank()

}