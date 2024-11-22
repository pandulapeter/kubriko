plugins {
    id("kubriko-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.plugins.keyboardInput)
            implementation(projects.plugins.shader)
            implementation(projects.tools.debugMenu)
            implementation(projects.tools.sceneEditor)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization)
        }
        desktopMain.dependencies {
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoPerformance"
}