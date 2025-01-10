plugins {
    id("kubriko-compose-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.examples.shared)
            implementation(projects.engine)
            implementation(projects.plugins.physics)
            implementation(projects.plugins.pointerInput)
            implementation(projects.tools.debugMenu)
            implementation(projects.tools.sceneEditor)
            implementation(compose.components.resources)
            implementation(compose.material3)
            implementation(libs.kotlinx.serialization)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoPhysics"
}