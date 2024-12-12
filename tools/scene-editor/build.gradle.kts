plugins {
    id("kubriko-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(projects.plugins.serialization)
            implementation(compose.components.resources)
        }
        val desktopMain by getting {
            dependencies {
                implementation(projects.plugins.collision)
                implementation(projects.plugins.keyboardInput)
                implementation(projects.plugins.persistence)
                implementation(projects.plugins.pointerInput)
                implementation(projects.tools.debugMenu)
                implementation(compose.material3)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlin.reflect)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.sceneEditor"
}
