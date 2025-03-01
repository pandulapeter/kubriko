/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui.licenses

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.other_licenses_apache_2_0
import kubriko.app.generated.resources.other_licenses_content
import kubriko.app.generated.resources.other_licenses_lgpl_2_1
import kubriko.app.generated.resources.other_licenses_mit
import kubriko.app.generated.resources.other_licenses_mpl_2_0
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


fun createLicensesScreenStateHolder(): LicensesScreenStateHolder = LicensesScreenStateHolderImpl()

@Composable
internal fun LicensesScreen(
    modifier: Modifier = Modifier,
    stateHolder: LicensesScreenStateHolder = createLicensesScreenStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    scrollState: ScrollState = rememberScrollState(),
) = Column(
    modifier = modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Right))
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    Text(
        style = MaterialTheme.typography.bodySmall,
        text = stringResource(Res.string.other_licenses_content),
    )
    Dependency.entries.groupBy { it.type }.forEach { (type, dependencies) ->
        Text(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            text = stringResource(type.licenseName),
        )
        val uriHandler = LocalUriHandler.current
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            dependencies.forEach { dependency ->
                Text(
                    modifier = Modifier
                        .clickable { uriHandler.openUri(dependency.url) }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    text = dependency.dependencyName,
                )
            }
        }
    }
}

private enum class LicenseType(
    val licenseName: StringResource,
) {
    APACHE_2_0(
        licenseName = Res.string.other_licenses_apache_2_0,
    ),
    LGPL_2_1(
        licenseName = Res.string.other_licenses_lgpl_2_1,
    ),
    MIT(
        licenseName = Res.string.other_licenses_mit,
    ),
    MPL_2_0(
        licenseName = Res.string.other_licenses_mpl_2_0,
    );
}

private enum class Dependency(
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
    JLAYER(
        dependencyName = "JLayer",
        url = "https://github.com/umjammer/jlayer/blob/master/LICENSE.txt",
        type = LicenseType.LGPL_2_1,
    ),
    SINGLE_PAGE_APPS_FOR_GITHUB_PAGES(
        dependencyName = "Single Page Apps for GitHub Pages",
        url = "https://github.com/rafgraph/spa-github-pages/blob/gh-pages/LICENSE",
        type = LicenseType.MIT,
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
    KUBRIKO(
        dependencyName = "Kubriko",
        url = "https://github.com/pandulapeter/kubriko/blob/main/LICENSE",
        type = LicenseType.MPL_2_0,
    );
}

sealed interface LicensesScreenStateHolder : StateHolder {

    companion object {
        @Composable
        fun areResourcesLoaded() = areStringResourcesLoaded()

        @Composable
        private fun areStringResourcesLoaded() = preloadedString(Res.string.other_licenses_content).value.isNotBlank()
                && preloadedString(Res.string.other_licenses_apache_2_0).value.isNotBlank()
                && preloadedString(Res.string.other_licenses_lgpl_2_1).value.isNotBlank()
                && preloadedString(Res.string.other_licenses_mit).value.isNotBlank()
                && preloadedString(Res.string.other_licenses_mpl_2_0).value.isNotBlank()
    }
}

private class LicensesScreenStateHolderImpl : LicensesScreenStateHolder {
    override val kubriko: Flow<Kubriko?> = emptyFlow()

    override fun dispose() = Unit
}