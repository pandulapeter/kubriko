plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.collision"
}