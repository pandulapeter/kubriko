plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            implementation(projects.plugins.audioPlayback)
            implementation(projects.plugins.collision)
            implementation(projects.plugins.keyboardInput)
            implementation(projects.plugins.persistence)
            implementation(projects.plugins.pointerInput)
            implementation(projects.plugins.shaders)
            implementation(projects.tools.uiComponents)
            implementation(compose.components.resources)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.gameWallbreaker"
}