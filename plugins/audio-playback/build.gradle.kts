plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
        }
        desktopMain.dependencies {
            implementation(libs.jlayer)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.audioPlayback"
}