plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.plugins.physics)
            implementation(projects.tools.debugMenu)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoPhysics"
}