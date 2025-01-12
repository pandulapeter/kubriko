plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.examples.shared)
            implementation(projects.engine)
            implementation(projects.plugins.audioPlayback)
            implementation(projects.tools.debugMenu)
            implementation(compose.components.resources)
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoAudio"
}