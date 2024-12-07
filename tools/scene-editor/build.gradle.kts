plugins {
    id("kubriko-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(projects.plugins.serialization)
            implementation(projects.plugins.keyboardInput)
            implementation(projects.plugins.pointerInput)
            implementation(projects.tools.debugMenu)
            implementation(compose.components.resources)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.material3)
                implementation(libs.kotlinx.serialization)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlin.reflect)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.sceneEditor"
}
