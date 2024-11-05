plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
            implementation(compose.runtime)
            api(compose.foundation)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.keyboardInputManager"
}