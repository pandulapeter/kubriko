plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(projects.plugins.physics)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.demoPhysics"
}