plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.examples.shared)
            implementation(projects.engine)
            implementation(projects.plugins.shader)
            implementation(compose.components.resources)
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoCustomShaders"
}