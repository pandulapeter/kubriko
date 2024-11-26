plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(projects.plugins.collision)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.physicsManager"
}