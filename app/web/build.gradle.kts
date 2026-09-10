/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            webpackTask {
                if (mode == KotlinWebpackConfig.Mode.PRODUCTION) {
                    sourceMaps = false
                }
            }
        }
        binaries.executable()
    }
    sourceSets {
        webMain.dependencies {
            implementation(projects.app.shared)
            implementation(projects.engine)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
    }
}

/**
 * Everything the Showcase waits for before drawing its first frame, as glob patterns relative to the distribution folder.
 * The app requests these one after the other, so without preloading each of them costs a full network round trip.
 * Every pattern must match at least one file so that a renamed or removed resource fails the build instead of silently
 * losing its preload.
 */
val webPreloadPatterns = listOf(
    "*.wasm",
    "composeResources/kubriko.tools.ui_components.generated.resources/font/*.ttf",
    "composeResources/*/values/*.cvr",
    "composeResources/kubriko.app.shared.generated.resources/drawable/*",
    "composeResources/kubriko.examples.demo_*.generated.resources/drawable/*.xml",
    "composeResources/kubriko.examples.demo_isometric_graphics.generated.resources/drawable/*.webp",
    "composeResources/kubriko.examples.test_*.generated.resources/drawable/*.xml",
)

/**
 * Injects `<link rel="preload">` tags into the distributed index.html so that the browser starts fetching the wasm
 * binaries and the first-frame resources while it is still parsing the HTML, in parallel instead of sequentially.
 * It also injects the uncompressed size of every preloaded file, which the loading screen's progress bar measures
 * the downloads against. The injected block is delimited by markers so that re-running the task replaces it.
 */
val injectWebPreloads = tasks.register("injectWebPreloads") {
    group = "distribution"
    description = "Adds preload links and a size table for the wasm binaries and first-frame resources to the distributed index.html."
    val distributionDirectory = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
    inputs.dir(distributionDirectory)
    outputs.upToDateWhen { false }
    doLast {
        val distributionFolder = distributionDirectory.get().asFile
        val preloadPaths = webPreloadPatterns.flatMap { pattern ->
            fileTree(distributionFolder) { include(pattern) }.files
                .ifEmpty { throw GradleException("No file in the web distribution matches the preload pattern '$pattern'.") }
                .map { it.relativeTo(distributionFolder).invariantSeparatorsPath }
        }.distinct()
        val indexFile = distributionFolder.resolve("index.html")
        val closingHeadTag = "</head>"
        val startMarker = "<!-- injectWebPreloads:start -->"
        val endMarker = "<!-- injectWebPreloads:end -->"
        val indexContent = indexFile.readText().replace(Regex("\\s*${Regex.escape(startMarker)}.*?${Regex.escape(endMarker)}", RegexOption.DOT_MATCHES_ALL), "")
        check(indexContent.contains(closingHeadTag)) { "The distributed index.html has no closing head tag to inject the preload links before." }
        val preloadLinks = preloadPaths.joinToString(separator = "") { path ->
            "    <link rel=\"preload\" href=\"$path\" as=\"fetch\" crossorigin=\"anonymous\">\n"
        }
        val preloadSizes = preloadPaths.associateWith { distributionFolder.resolve(it).length() }
        val sizeTable = groovy.json.JsonOutput.toJson(preloadSizes)
        val injectedBlock = "    $startMarker\n$preloadLinks    <script>window.kubrikoResourceSizes = $sizeTable;</script>\n    $endMarker\n"
        indexFile.writeText(indexContent.replaceFirst(closingHeadTag, injectedBlock + closingHeadTag))
        val totalSizeInKilobytes = preloadSizes.values.sum() / 1024
        logger.lifecycle("Injected ${preloadPaths.size} preload links ($totalSizeInKilobytes KB) into ${indexFile.name}.")
    }
}

tasks.matching { it.name == "wasmJsBrowserDistribution" }.configureEach {
    finalizedBy(injectWebPreloads)
}
