plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.examples.shared)
            implementation(projects.engine)
            implementation(projects.plugins.audioPlayer)
            implementation(projects.plugins.collision)
            implementation(projects.plugins.keyboardInput)
            implementation(projects.plugins.persistence)
            implementation(projects.plugins.pointerInput)
            implementation(projects.plugins.shader)
            implementation(compose.components.resources)
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.gameWallbreaker"
}