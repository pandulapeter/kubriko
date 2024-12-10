plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.plugins.shader)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoCollisions"
}