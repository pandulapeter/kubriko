plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.shared"
}