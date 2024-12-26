plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(compose.components.resources)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.sprites"
}