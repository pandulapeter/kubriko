import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
     implementation(libs.gradle)
     implementation(libs.kotlin)
}

gradlePlugin {
    plugins {
        register("library") {
            id = "kubriko-library"
            implementationClass = "com.pandulapeter.kubriko.buildLogic.plugins.MultiplatformLibraryPlugin"
        }
        register("application") {
            id = "kubriko-application"
            implementationClass = "com.pandulapeter.kubriko.buildLogic.plugins.MultiplatformApplicationPlugin"
        }
    }
}