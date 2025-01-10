plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            implementation(projects.tools.logger)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.shaders"
}