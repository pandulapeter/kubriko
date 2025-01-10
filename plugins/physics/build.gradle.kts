plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(projects.plugins.collision)
            implementation(projects.tools.logger)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.physics"
}