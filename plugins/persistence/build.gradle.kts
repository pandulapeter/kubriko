plugins {
    id("kubriko-compose-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.persistence"
}