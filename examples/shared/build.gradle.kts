plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(libs.kotlinx.coroutines)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.shared"
}