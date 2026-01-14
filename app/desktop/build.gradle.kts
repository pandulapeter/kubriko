/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.desktop)
}

dependencies {
    implementation(projects.app.shared)
    implementation(projects.engine)
    implementation(projects.examples.demoIsometricGraphics)
    implementation(projects.examples.demoPerformance)
    implementation(projects.examples.demoPhysics)
    implementation(projects.examples.gameAnnoyedPenguins)
    implementation(projects.examples.gameBlockysJourney)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
}

compose.desktop {
    application {
        mainClass = "com.pandulapeter.kubrikoShowcase.KubrikoShowcaseAppKt"
        nativeDistributions {
            packageName = "Kubriko Showcase"
            packageVersion = project.findProperty("showcase.versionName").toString()
            description = "Demo app showcasing the feature set of the Kubriko game engine"
            copyright = "© 2025-2026 Pandula Péter. All rights reserved."
            macOS {
                targetFormats(TargetFormat.Dmg)
                iconFile.set(project.file("icon.icns"))
                bundleID = "com.pandulapeter.kubrikoShowcase"
                dockName = "Kubriko Showcase"
                signing {
                    sign.set(true)
                    identity.set("PETER PANDULA")
                }
                notarization {
                    val providers = project.providers
                    appleID.set(providers.environmentVariable("NOTARIZATION_APPLE_ID"))
                    password.set(providers.environmentVariable("NOTARIZATION_PASSWORD"))
                    teamID.set(providers.environmentVariable("NOTARIZATION_TEAM_ID"))
                }
            }
            windows {
                targetFormats(TargetFormat.Exe)
                iconFile.set(project.file("icon.ico"))
                menuGroup = "Kubriko"
            }
            linux {
                targetFormats(TargetFormat.Deb)
                iconFile.set(project.file("icon.png"))
                debMaintainer = "pandulapeter@gmail.com"
                menuGroup = "kubriko-showcase"
            }
            buildTypes.release.proguard {
                configurationFiles.from(project.file("proguard-rules.pro"))
                isEnabled.set(false) // TODO: Wait for a ProGuard configuration fix
                optimize.set(true)
                obfuscate.set(true)
            }
        }
    }
}