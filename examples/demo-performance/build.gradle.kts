plugins {
    id("kubriko-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.tools.debugMenu)
            implementation(projects.tools.sceneEditor)
            implementation(compose.components.resources)
            implementation(compose.material3)
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