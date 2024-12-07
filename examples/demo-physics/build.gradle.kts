plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.plugins.physics)
            implementation(projects.plugins.pointerInput)
            implementation(projects.tools.debugMenu)
            implementation(compose.components.resources)
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoPhysics"
}