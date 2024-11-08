import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("kubriko-application")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.plugins.shader)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubrikoPong"
    defaultConfig {
        applicationId = "com.pandulapeter.kubrikoPong"
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
}

compose.desktop {
    application {
        mainClass = "com.pandulapeter.kubrikoPong.GameKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.pandulapeter.kubrikoPong"
            packageVersion = "1.0.0"
            buildTypes.release.proguard {
                configurationFiles.from(project.file("proguard-rules.pro"))
                isEnabled.set(true)
                obfuscate.set(true)
            }
        }
    }
}
