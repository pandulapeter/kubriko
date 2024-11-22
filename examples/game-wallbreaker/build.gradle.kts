plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubrikoWallbreaker"
}