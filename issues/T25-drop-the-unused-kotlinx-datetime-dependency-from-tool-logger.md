# Drop the unused kotlinx-datetime dependency from tool-logger

**Kind:** build  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** tool-logger (published; its POM loses a runtime dependency)
**Files:** tools/logger/build.gradle.kts

## Problem
`tools/logger/build.gradle.kts:26` declares `implementation(libs.kotlinx.datetime)`, but the module's only source file
(`Logger.kt`) uses `kotlin.time.Clock` and imports nothing from `kotlinx.datetime`. Because `engine` exposes the logger with
`api(projects.tools.logger)`, every game built on Kubriko pulls kotlinx-datetime at runtime for nothing.

Verified that nothing relies on getting it transitively: the only `import kotlinx.datetime` in this repo is
`tools/debug-menu/.../ui/LogEntry.kt`, and `tools/debug-menu/build.gradle.kts` declares `implementation(libs.kotlinx.datetime)`
itself; no example, app module or Tesselar file imports `kotlinx.datetime`. An `implementation` dependency is published in the
POM's runtime scope only, so no consumer could have compiled against it through tool-logger.

## Fix
Delete the `implementation(libs.kotlinx.datetime)` line. Keep the `libs.versions.toml` entry (debug-menu still uses it).

## Behaviour
No code change. The published `tool-logger` POM/Gradle module metadata no longer lists kotlinx-datetime; apps that use the debug
menu still get it from `tool-debug-menu`.

## Public API
None (no declaration changes). The release notes may mention the dropped transitive runtime dependency.

## Tests
The existing ones.

## Verify
`./gradlew :tools:logger:compileKotlinDesktop :tools:logger:compileKotlinWasmJs :tools:debug-menu:compileKotlinDesktop :engine:desktopTest`

## Manual check
None.
