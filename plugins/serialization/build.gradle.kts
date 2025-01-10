plugins {
    id("kubriko-compose-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(libs.kotlinx.serialization)
            implementation(projects.tools.logger)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.serialization"
}