plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            implementation(projects.plugins.persistence)
            implementation(projects.tools.uiComponents)
            implementation(compose.components.resources)
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.debugMenu"
}