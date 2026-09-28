# Stop `build` from linking iOS and Wasm test binaries (and from running the repo-wide npm install) for test runs that are already disabled

**Challenged:** amended — probe confirmed 478→312 dry-run lines (incl. 5 JVM warning lines), `compileTestKotlin{IosArm64,IosSimulatorArm64,WasmJs}` + `desktopTest` executed, link/executable/sync/run SKIPPED, `allTests`/`check` wiring intact (repo-wide `check allTests` dry run loses only 39 `kotlinWasmNodeJsSetup`); fixed the second verification command, which expected 0 but the main compilations' `wasmJsPackageJson`/`wasmJsPublicPackageJson` (engine + logger) legitimately remain.

**Kind:** build  ·  **Severity:** low  ·  **Platforms:** iOS, Web (build time only)
**Files:**
- `gradle/build-logic/src/main/kotlin/com/pandulapeter/kubriko/buildLogic/extensions/KotlinMultiplatform.kt`
- `gradle/build-logic/CLAUDE.md` (one line under "KMP targets" / "Gotchas")

## Problem

`configureKotlinMultiplatform` disables the iOS and browser test runs (`KotlinMultiplatform.kt` at 76992502):

```kotlin
// Running these needs a browser or an iOS simulator; their compilations still run, so commonTest must compile everywhere.
tasks.withType(KotlinJsTest::class.java).configureEach { enabled = false }
tasks.withType(KotlinNativeTest::class.java).configureEach { enabled = false }
```

Gradle still runs the dependencies of a disabled task. `check` → `allTests` → `iosSimulatorArm64Test` / `wasmJsTest` → `wasmJsBrowserTest` therefore still pulls in, in every module that applies `kubriko-library` / `kubriko-compose-library`:

- `linkDebugTestIosSimulatorArm64`, which links a native test executable. `iosArm64` has no test binary because it is not host-runnable.
- `compileTestDevelopmentExecutableKotlinWasmJs` and `wasmJsTestTestDevelopmentExecutableCompileSync`, which build and sync a Wasm test executable.
- Through `wasmJsBrowserTest`'s explicit `dependsOn`, the root `kotlinWasmNpmInstall` and everything it needs: `kotlinWasmNodeJsSetup`, `kotlinWasmYarnSetup`, `kotlinWasmRestoreYarnLock`/`StoreYarnLock`, `kotlinWasmPackageJsonUmbrella`, `wasmRootPackageJson`, and a `wasmJsPackageJson` + `wasmJsTestPackageJson` + public variants **for every project in the repo**, plus each project's `serializeSwiftPMDependenciesMetadataForLockFiles`.

Nothing consumes any of these outputs. Measured at 76992502:

- `./gradlew :engine:build --dry-run -q` lists **473** tasks. Among them are `:engine:linkDebugTestIosSimulatorArm64`, `:engine:compileTestDevelopmentExecutableKotlinWasmJs`, `:engine:wasmJsTestTestDevelopmentExecutableCompileSync` and `:kotlinWasmNpmInstall`, plus about 200 `*PackageJson`/`SwiftPM` tasks from unrelated projects.
- `./gradlew build --dry-run -q` lists **9017** tasks, including **40** `linkDebugTestIosSimulatorArm64`, **40** `compileTestDevelopmentExecutableKotlinWasmJs` and **41** `*ExecutableCompileSync`. There is one of each per convention-plugin module (engine, plugins, tools, examples, `app:shared`), plus `app:ios`/`app:web`, which don't use the convention plugin.
- Profiled in a probe after deleting their outputs, `:engine:linkDebugTestIosSimulatorArm64` took **2.95 s** and `:engine:compileTestDevelopmentExecutableKotlinWasmJs` took **1.28 s**. After a probe build with the fix, restoring the old build logic and re-running `:engine:build --no-build-cache` executed 182 extra tasks: the link, the Wasm executable and sync, the npm install and about 170 package.json/SwiftPM tasks.

The run tasks themselves are already disabled, so nothing consumes the binaries. At 76992502 a real `:engine:build` reports `:engine:iosSimulatorArm64Test SKIPPED` and `:engine:wasmJsBrowserTest SKIPPED`.

## Fix

Keep every `compileTestKotlin<Target>` task, so commonTest still compiles for iOS and Wasm. Drop the run tasks' dependencies and disable the test-binary producers. Replace the two lines quoted above with:

```kotlin
// Running these needs a browser or an iOS simulator; their compilations still run, so commonTest must compile everywhere.
// A disabled task's dependencies still run, so the runs also drop their dependencies (the repo-wide npm install) and
// the test binaries they would have consumed are not linked.
tasks.withType(KotlinJsTest::class.java).configureEach {
    enabled = false
    setDependsOn(emptyList<Any>())
}
tasks.withType(KotlinNativeTest::class.java).configureEach {
    enabled = false
    setDependsOn(emptyList<Any>())
}
tasks.withType(KotlinNativeLink::class.java).matching { it.binary is TestExecutable }.configureEach { enabled = false }
tasks.withType(KotlinJsIrLink::class.java).matching { it.name.startsWith("compileTest") }.configureEach { enabled = false }
tasks.matching { it.name.startsWith("wasmJsTestTest") && it.name.endsWith("ExecutableCompileSync") }.configureEach { enabled = false }
```

Add these imports:

```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
```

Keep the existing `checkComposeUiTestConfigurationForWasmJs` line. Follow the `code-style` skill: keep the comment, and trim it if it reads as narration.

Why it has two parts, verified in a probe worktree with Kotlin 2.4.20 and Gradle 9.7.1:

- `setDependsOn(emptyList())` removes the **explicit** dependencies: the npm install chain and the other projects' package.json/SwiftPM tasks. With it, `:engine:build --dry-run` drops from 473 to **307** tasks. It can't remove the link and executable tasks, because the test tasks reach those **implicitly** through their executable input properties.
- The link and executable tasks therefore stay in the graph but are disabled. Their own dependencies are the `compileTestKotlin*` tasks, which still run, and that is what we want. A real `:engine:build` in the probe showed `compileTestKotlinIosArm64`, `compileTestKotlinIosSimulatorArm64`, `compileTestKotlinWasmJs` executed, `linkDebugTestIosSimulatorArm64`, `compileTestDevelopmentExecutableKotlinWasmJs`, `wasmJsTestTestDevelopmentExecutableCompileSync`, `iosSimulatorArm64Test` and `wasmJsBrowserTest` SKIPPED, and `desktopTest` ran. BUILD SUCCESSFUL.

Rejected alternatives:

- Only disabling the link tasks by name leaves the npm install in every per-module build.
- `wasmJs { browser { testTask { enabled = false } } }` is the same as the current `enabled = false` and keeps the dependencies.
- Removing test binaries or `testRuns` from the targets is not supported by the KGP DSL (test binaries are created by KGP for host-testable targets).

Scope:

- `app:ios` and `app:web` don't apply the convention plugins, so they keep their own (source-less) test link/executable tasks. In the probe, `./gradlew build --dry-run` showed their task lists unchanged.
- In the full `build`, `kotlinWasmNpmInstall` still runs because `app:web`'s distribution needs it. The full-build dry run only loses the 39 per-module `kotlinWasmNodeJsSetup` entries (9017 → 8978). The real saving there is the 40 iOS test links and 40 Wasm test executables that no longer execute.
- `iosArm64` never had a test binary, so there is nothing to disable for it.

Optionally, add a sentence to `gradle/build-logic/CLAUDE.md` saying that iOS/Wasm test runs and their test binaries are disabled while their test compilations still run in `build`.

## Tests

No unit test applies. Verify with Gradle, one command at a time:

```bash
./gradlew :engine:build --dry-run -q | wc -l                      # was 473; expect ~307 (plus any JVM warning lines, e.g. 478 → 312)
./gradlew :engine:build --dry-run -q | grep -E "NpmInstall|TestPackageJson|TestPublicPackageJson" | wc -l   # expect 0 (the main compilations' wasmJsPackageJson/wasmJsPublicPackageJson stay)
./gradlew :engine:build --console=plain | grep -E "compileTestKotlin|linkDebugTest|TestDevelopmentExecutable|iosSimulatorArm64Test|wasmJsBrowserTest|desktopTest"
```

The third command must show `compileTestKotlinIosArm64`, `compileTestKotlinIosSimulatorArm64`, `compileTestKotlinWasmJs` and `desktopTest` executed (or UP-TO-DATE/FROM-CACHE), and the link, executable, sync and run tasks `SKIPPED`.

To confirm that commonTest still compiles everywhere, introduce a deliberate type error in any module's `commonTest` locally. `./gradlew :engine:compileTestKotlinWasmJs` must then fail. Revert it afterwards.

Also run `./gradlew build --dry-run -q > after.txt` and diff it against a run from before the change: only `*:kotlinWasmNodeJsSetup` lines should disappear, and every `:app:ios:`/`:app:web:` line should be unchanged.

Optional timing: in a clean clone, run `./gradlew :engine:build` before and after the change, each time after `rm -rf engine/build/bin/iosSimulatorArm64/debugTest engine/build/compileSync/wasmJs/test`. For one module, expect about 4 s less task time plus the npm install. Across a full `build`, 40 modules each skip one link and one Wasm executable.

## Manual check

After a KGP upgrade, re-check the dry-run counts. The Wasm task names (`compileTest…ExecutableKotlinWasmJs`, `wasmJsTestTest…ExecutableCompileSync`) are matched by name and would silently stop matching if KGP renames them. The only cost of that is lost savings, not a broken build.
