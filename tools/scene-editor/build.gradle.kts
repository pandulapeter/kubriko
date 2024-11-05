plugins {
    id("kubriko-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.plugins.serializationManager)
            implementation(compose.components.resources)
            implementation(projects.engine)
            implementation(projects.plugins.keyboardInputManager)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(libs.kotlinx.serialization)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlinx.coroutines)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.sceneEditor"
}
