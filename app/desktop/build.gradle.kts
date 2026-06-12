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
import org.jetbrains.compose.desktop.application.tasks.AbstractProguardTask

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.desktop)
}

// The obfuscation mapping file is needed to make sense of obfuscated stack traces from crash
// reports. ProGuard resolves relative paths against an unpredictable working directory, so the
// -printmapping rule needs an absolute path, which has to be generated at build time.
val generateProguardMappingRules by tasks.registering {
    val rulesFile = layout.buildDirectory.file("compose/proguard/mapping-rules.pro")
    val mappingFile = layout.buildDirectory.file("compose/proguard/mapping.txt")
    outputs.file(rulesFile)
    doLast {
        rulesFile.get().asFile.writeText("-printmapping '${mappingFile.get().asFile.absolutePath}'\n")
    }
}

// ProGuard needs the jmods directory of a full JDK to resolve java.** references, but the JetBrains
// Runtime that usually backs the Gradle daemon ships without jmods. Resolve a complete JDK 21
// (auto-provisioned through the Foojay toolchain resolver if needed) just for the ProGuard task.
// The Compose plugin assigns its own javaHome when it creates the task in afterEvaluate, so this
// override must be applied from a later afterEvaluate block to win.
afterEvaluate {
    tasks.withType<AbstractProguardTask>().configureEach {
        javaHome.set(
            javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.ADOPTIUM)
            }.map { it.metadata.installationPath.asFile.absolutePath }
        )
    }
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
                configurationFiles.from(project.file("proguard-rules.pro"), generateProguardMappingRules)
                isEnabled.set(true)
                optimize.set(true)
                obfuscate.set(true)
                joinOutputJars.set(true)
            }
        }
    }
}