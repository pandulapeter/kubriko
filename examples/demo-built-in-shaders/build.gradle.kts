plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.examples.shared)
            implementation(projects.engine)
            implementation(projects.plugins.shader)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoCollisions"
}