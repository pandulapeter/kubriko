plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.examples.shared)
            implementation(projects.engine)
            implementation(projects.plugins.keyboardInput)
            implementation(projects.plugins.pointerInput)
            implementation(projects.tools.debugMenu)
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoInput"
}