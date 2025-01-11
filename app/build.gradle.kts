import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("kubriko-application")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.examples.demoAudio)
            implementation(projects.examples.demoContentShaders)
            implementation(projects.examples.demoInput)
            implementation(projects.examples.demoPerformance)
            implementation(projects.examples.demoPhysics)
            implementation(projects.examples.demoShaderAnimations)
            implementation(projects.examples.gameSpaceSquadron)
            implementation(projects.examples.gameWallbreaker)
            implementation(projects.tools.debugMenu)
            implementation(projects.tools.uiComponents)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashScreen)
            implementation(libs.google.material)
        }
        val desktopMain by getting {
            dependencies {
                implementation(projects.engine)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubrikoShowcase"
    defaultConfig {
        applicationId = "com.pandulapeter.kubrikoShowcase"
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
}

compose.desktop {
    application {
        mainClass = "com.pandulapeter.kubrikoShowcase.KubrikoShowcaseAppKt"
        nativeDistributions {
            packageName = "com.pandulapeter.kubrikoShowcase"
            packageVersion = "1.0.0"
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            macOS { iconFile.set(project.file("icon.icns")) }
            windows { iconFile.set(project.file("icon.ico")) }
            linux { iconFile.set(project.file("icon.png")) }
            buildTypes.release.proguard {
                configurationFiles.from(project.file("proguard-rules.pro"))
                isEnabled.set(true)
                obfuscate.set(true)
            }
        }
    }
}
