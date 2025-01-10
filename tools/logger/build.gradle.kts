plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.logger"
}