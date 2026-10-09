/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui.licenses

internal enum class Dependency(
    val dependencyName: String,
    val url: String,
    val type: LicenseType,
) {
    ANDROID_X_ACTIVITY(
        dependencyName = "AndroidX Activity",
        url = "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    ANDROID_X_CORE_SPLASH_SCREEN(
        dependencyName = "AndroidX Core Splash Screen",
        url = "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    ANDROID_X_LIFECYCLE(
        dependencyName = "AndroidX Lifecycle",
        url = "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    APACHE_COMMONS_LANG(
        dependencyName = "Apache CommonsLang",
        url = "https://github.com/apache/commons-lang/blob/master/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    BUILD_KONFIG(
        dependencyName = "BuildKonfig",
        url = "https://github.com/yshrsmz/BuildKonfig/blob/master/LICENSE",
        type = LicenseType.APACHE_2_0,
    ),
    COMPOSE_MULTIPLATFORM(
        dependencyName = "Compose Multiplatform",
        url = "https://github.com/JetBrains/compose-multiplatform/blob/master/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    GRADLE(
        dependencyName = "Gradle",
        url = "https://github.com/gradle/gradle/blob/master/LICENSE",
        type = LicenseType.APACHE_2_0,
    ),
    GRADLE_MAVEN_PUBLISH_PLUGIN(
        dependencyName = "Gradle Maven Publish Plugin",
        url = "https://github.com/vanniktech/gradle-maven-publish-plugin/blob/main/LICENSE",
        type = LicenseType.APACHE_2_0,
    ),
    KOTLIN(
        dependencyName = "Kotlin",
        url = "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    KOTLINX_COROUTINES(
        dependencyName = "KotlinX Coroutines",
        url = "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    KOTLINX_DATE_TIME(
        dependencyName = "KotlinX DateTime",
        url = "https://github.com/Kotlin/kotlinx-datetime/blob/master/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    KOTLINX_IMMUTABLE_COLLECTIONS(
        dependencyName = "KotlinX Immutable Collections",
        url = "https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    KOTLINX_SERIALIZATION(
        dependencyName = "Serialization",
        url = "https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt",
        type = LicenseType.APACHE_2_0,
    ),
    MATERIAL_COMPONENTS(
        dependencyName = "Material Components",
        url = "https://github.com/material-components/material-components-android/blob/master/LICENSE",
        type = LicenseType.APACHE_2_0,
    ),
    MATERIAL_DESIGN_ICONS(
        dependencyName = "Material Design Icons",
        url = "https://pictogrammers.com/docs/general/license/",
        type = LicenseType.APACHE_2_0,
    ),
    FREESOUND_CC0(
        dependencyName = "freesound.org",
        url = "https://creativecommons.org/publicdomain/zero/1.0/",
        type = LicenseType.CC0_1_0,
    ),
    OPEN_GAME_ART(
        dependencyName = "opengameart.org",
        url = "https://creativecommons.org/publicdomain/zero/1.0/",
        type = LicenseType.CC0_1_0,
    ),
    FREESOUND_CCBY(
        dependencyName = "freesound.org",
        url = "https://creativecommons.org/licenses/by/4.0/",
        type = LicenseType.CCBY_4_0,
    ),
    JLAYER(
        dependencyName = "JLayer",
        url = "https://github.com/umjammer/jlayer/blob/master/LICENSE.txt",
        type = LicenseType.LGPL_2_1,
    ),
    JPHYSICS(
        dependencyName = "JPhysics",
        url = "https://github.com/HaydenMarshalla/JPhysics/blob/master/LICENSE",
        type = LicenseType.MIT,
    ),
    KPHYSICS(
        dependencyName = "KPhysics",
        url = "https://github.com/KPhysics/KPhysics/blob/master/LICENSE",
        type = LicenseType.MIT,
    ),
    SINGLE_PAGE_APPS_FOR_GITHUB_PAGES(
        dependencyName = "Single Page Apps for GitHub Pages",
        url = "https://github.com/rafgraph/spa-github-pages/blob/gh-pages/LICENSE",
        type = LicenseType.MIT,
    ),
    KUBRIKO(
        dependencyName = "Kubriko",
        url = "https://github.com/pandulapeter/kubriko/blob/main/LICENSE",
        type = LicenseType.MPL_2_0,
    ),
}
