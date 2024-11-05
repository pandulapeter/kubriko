package com.pandulapeter.kubriko.convention.plugins

import com.android.build.gradle.LibraryExtension
import com.pandulapeter.kubriko.convention.extensions.configureKotlinAndroid
import com.pandulapeter.kubriko.convention.extensions.configureKotlinMultiplatform
import com.pandulapeter.kubriko.convention.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MultiplatformLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("kotlin-multiplatform").get().get().pluginId)
            apply(libs.findPlugin("androidLibrary").get().get().pluginId)
            apply(libs.findPlugin("compose").get().get().pluginId)
            apply(libs.findPlugin("compose-compiler").get().get().pluginId)
        }
        extensions.configure<KotlinMultiplatformExtension>(::configureKotlinMultiplatform)
        extensions.configure<LibraryExtension>(::configureKotlinAndroid)
    }
}