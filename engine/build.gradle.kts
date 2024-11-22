plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(compose.foundation)
            api(compose.runtime)
            api(libs.kotlinx.immutable)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.engine"
}
