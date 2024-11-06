plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            implementation(compose.material)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.debugMenu"
}