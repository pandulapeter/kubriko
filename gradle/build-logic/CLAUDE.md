<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# CLAUDE.md — gradle/build-logic

Convention plugins for all Kubriko modules. The `includeBuild("gradle")` in root `settings.gradle.kts` makes these available by ID everywhere.

## Convention plugins

| Plugin ID | Class | Purpose |
|---|---|---|
| `kubriko-library` | `LibraryPlugin` | KMP library with Android + Desktop (JVM) + iOS (arm64/simulatorArm64) + Wasm/JS targets. No Compose. |
| `kubriko-compose-library` | `ComposeLibraryPlugin` | Same targets as above plus Compose Multiplatform and Compose compiler plugins. |
| `kubriko-public-artifact` | `PublicArtifactPlugin` | Adds vanniktech Maven Publish; exposes `artifactMetadata { artifactId = "..." }` DSL. Always paired with one of the above. |

## KMP targets configured by both library plugins

- Android (`com.android.kotlin.multiplatform.library`; minSdk from version catalog, compileSdk from version catalog)
- Desktop JVM (`jvm("desktop")`; JVM runs with `-XX:+UseZGC`)
- iOS: `iosArm64` + `iosSimulatorArm64` (static framework named `ComposeApp`)
- Web: `wasmJs { browser(); binaries.executable() }` (experimental DSL, `@OptIn(ExperimentalWasmDsl::class)`)

All targets use JDK 21 toolchain (`jvmToolchain(21)`).

## Version constraints

- Build-logic itself compiles against **JDK 17** (`sourceCompatibility`, `targetCompatibility`, `jvmTarget` all set to 17).
- Module sources compile against **JDK 21** (toolchain set in `KotlinMultiplatform.kt`).
- Kotlin, Compose Multiplatform, and AGP versions come from `gradle/libs.versions.toml`.

## New published module — minimal build.gradle.kts

```kotlin
plugins {
    id("kubriko-compose-library")   // or kubriko-library if no Compose needed
    id("kubriko-public-artifact")
}

artifactMetadata {
    artifactId = "plugin-my-feature"   // becomes io.github.pandulapeter.kubriko:plugin-my-feature
}

kotlin {
    android {
        namespace = "com.pandulapeter.kubriko.myFeature"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
        }
    }
}
```

Then add the Gradle path (e.g. `:plugins:my-feature`) to the `include(...)` block in root `settings.gradle.kts`.

## Non-published module (example / internal)

Omit `kubriko-public-artifact` and the `artifactMetadata` block entirely.

## Feature-flag-driven dependency selection

Showcase flags in `gradle.properties` are consumed at configuration time via `project.findProperty(...)`. The pattern is:

```kotlin
implementation(
    if (project.findProperty("showcase.isDebugMenuEnabled") == "true")
        projects.tools.debugMenu
    else
        projects.tools.debugMenuNoop
)
```

Available flags (all `Boolean` strings): `showcase.areTestExamplesEnabled`, `showcase.isDebugMenuEnabled`, `showcase.isSceneEditorEnabled`, `showcase.shouldShowUnfinishedGames`.

The `app:shared` module also uses `codingfeline-buildkonfig` to bake these flags into a `BuildConfig` object at compile time.

## Publishing

`kubriko-public-artifact` wires vanniktech's `MavenPublishBaseExtension` in an `afterEvaluate` block:
- groupId is always `io.github.pandulapeter.kubriko`
- version is taken from `rootProject.version` (set by `library.version` in `gradle.properties`)
- Publishes to Maven Central with signed publications

Run: `./gradlew publishToMavenCentral --no-configuration-cache`

## Gotchas

- The `android { namespace = "..." }` block is required by AGP even for KMP library modules — do not omit it.
- `artifactMetadata` is resolved in `afterEvaluate`; accessing it outside that hook will see null values.
- iOS simulator target is `iosSimulatorArm64` only (no x86_64); Intel Mac simulators are not supported.
- The build-logic `build.gradle.kts` compiles at JVM 17, but every module's Kotlin source tree uses a JDK 21 toolchain — mismatching these breaks the includeBuild compilation.
- Every source file must start with the MPL-2.0 license header.
