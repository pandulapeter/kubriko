plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(compose.components.resources)
            implementation(projects.tools.logger)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.sprites"
}