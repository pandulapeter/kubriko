plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")
    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
        }
        val desktopMain by getting {
            dependencies {
                implementation(projects.engine)
                api(projects.plugins.sceneSerializer)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(libs.kotlinx.serialization)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}
