plugins {
    id("kubriko-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
            implementation(projects.engine)
            api(projects.plugins.actorSerializer)
            implementation(projects.plugins.inputManager)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(libs.kotlinx.serialization)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlinx.coroutines)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.sceneEditor"
}
