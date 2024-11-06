import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("kubriko-application")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.plugins.keyboardInputManager)
            implementation(projects.plugins.serializationManager)
            implementation(projects.plugins.shaderManager)
            implementation(projects.tools.debugMenu)
            implementation(projects.tools.sceneEditor)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubrikoPerformanceTest"
    defaultConfig {
        applicationId = "com.pandulapeter.kubrikoPerformanceTest"
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
}

compose.desktop {
    application {
        mainClass = "com.pandulapeter.kubrikoPerformanceTest.GameKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.pandulapeter.kubrikoPerformanceTest"
            packageVersion = "1.0.0"
            buildTypes.release.proguard {
                configurationFiles.from(project.file("proguard-rules.pro"))
                isEnabled.set(true)
                obfuscate.set(true)
            }
        }
    }
}
