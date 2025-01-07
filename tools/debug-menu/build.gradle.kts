plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            implementation(projects.examples.shared)
            implementation(compose.components.resources)
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.debugMenu"
}