plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.plugins.collision)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.gameWallbreaker"
}