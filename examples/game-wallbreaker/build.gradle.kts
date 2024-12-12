plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.plugins.audioPlayer)
            implementation(projects.plugins.collision)
            implementation(projects.plugins.keyboardInput)
            implementation(projects.plugins.persistence)
            implementation(projects.plugins.shader)
            implementation(compose.components.resources)
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.gameWallbreaker"
}