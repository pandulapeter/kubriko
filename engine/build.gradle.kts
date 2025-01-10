plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(compose.foundation)
            api(compose.runtime)
            api(libs.kotlinx.coroutines)
            api(libs.kotlinx.immutable)
            api(projects.tools.logger)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.engine"
}
