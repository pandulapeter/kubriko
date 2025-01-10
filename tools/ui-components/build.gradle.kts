plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(compose.material3)
            implementation(compose.components.resources)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.shared"
}