plugins {
    id("kubriko-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(libs.kotlinx.serialization)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.serialization"
}