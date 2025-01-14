plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.datetime)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.logger"
}