# Stop linking iOS frameworks and Wasm executables for every library module

**Challenged:** sound — the only consumers are `app:ios` (own `ComposeApp` framework, Xcode calls `:app:ios:embedAndSignAppleFrameworkForXcode`) and `app:web` (own executable; CI ships only its distribution); Compose resources of libraries reach both apps through the klib resource publication, not through a library binary; no module declares linker options or cinterops; Tesselar consumes Maven artifacts and declares its own binaries; plan `00`'s disabled test tasks and the Wasm/iOS test compilations do not depend on `executable()` or the framework.

**Kind:** build performance  ·  **Severity:** medium (every `build` of a module pays for binaries nothing uses)  ·  **Platforms:** build only (iOS, Web)
**Files:** `gradle/build-logic/src/main/kotlin/com/pandulapeter/kubriko/buildLogic/extensions/KotlinMultiplatform.kt`, `gradle/build-logic/CLAUDE.md`

Added during execution on 2026-09-28, after the full build with plan `00` in place took well over ten minutes. It is
not part of the reviewed findings and is not in any lane: it touches only build-logic, which no lane edits.

## Problem

`configureKotlinMultiplatform` (applied by both `kubriko-library` and `kubriko-compose-library`, so by every engine,
plugin, tool, example and `app:shared` module) declares final binaries as if each module were an app:

```kotlin
listOf(
    iosArm64(),
    iosSimulatorArm64()
).forEach { iosTarget ->
    iosTarget.binaries.framework {
        baseName = "ComposeApp"
        isStatic = true
    }
}
@OptIn(ExperimentalWasmDsl::class)
wasmJs {
    browser()
    binaries.executable()
}
```

So `./gradlew build` (and `./gradlew :<module>:build`, the lanes' compile check) runs, for every module, a debug and
a release Kotlin/Native framework link for both iOS targets (`linkDebugFrameworkIosArm64`,
`linkReleaseFrameworkIosSimulatorArm64`, …) and a production Wasm executable compile and optimisation
(`compileProductionExecutableKotlinWasmJs`, plus the development variant and the distribution tasks). Native release
links are the slowest tasks in the toolchain. The partial log of that build already held 23 link tasks and 11
production Wasm executables before it reached the app modules.

None of these binaries is consumed:
- iOS: Xcode runs `./gradlew :app:ios:embedAndSignAppleFrameworkForXcode`
  (`app/ios/iosApp/iosApp.xcodeproj/project.pbxproj`), and `app/ios/build.gradle.kts` declares its own
  `ComposeApp` framework. `app:ios` depends on `app:shared` as a klib.
- Web: `app/web/build.gradle.kts` declares its own `wasmJs { browser { … }; binaries.executable() }`, and
  `.github/workflows/showcase-publish-web.yml` ships `app/web/build/dist/wasmJs/productionExecutable`.
- Maven Central: the published artifacts are klibs; frameworks and executables are never part of a publication
  (`library-publish.yml` runs `publishToMavenCentral`).
- Tesselar (`../Tesselar/app/ios`, `../Tesselar/app/web`) declares its own framework and executable too.
- No other build file declares `binaries.framework` or `binaries.executable`.

## Fix

In `configureKotlinMultiplatform`:

1. Declare the iOS targets without a framework:
   ```kotlin
   iosArm64()
   iosSimulatorArm64()
   ```
2. Drop `binaries.executable()` from the `wasmJs` block and keep `browser()` (the Wasm test compilations and the
   disabled browser test task from plan `00` need the browser environment):
   ```kotlin
   @OptIn(ExperimentalWasmDsl::class)
   wasmJs {
       browser()
   }
   ```
3. Remove imports that become unused, if any.
4. `gradle/build-logic/CLAUDE.md` → "KMP targets configured by both library plugins": the iOS line becomes
   `iosArm64` + `iosSimulatorArm64` (klibs only; the final framework is declared by the app module, e.g. `app:ios`),
   and the Web line becomes `wasmJs { browser() }` (the executable is declared by the app module, e.g. `app:web`).

Drop this plan if a module other than `app:ios`/`app:web` turns out to link or run one of these binaries (a
`grep -rn "Framework\|ExecutableKotlinWasmJs\|wasmJsBrowser" --include='*.kts' --include='*.yml' --include='*.pbxproj'`
outside `build/` finds a reference to a library module's binary).

## Tests

No unit test applies. Verify instead:
1. Before the change, save `./gradlew :engine:build --dry-run | grep -cE "link.*Framework|ExecutableKotlinWasmJs"`;
   after it, the same count is `0`.
2. Publications are unchanged: run `./gradlew :engine:generateMetadataFileForKotlinMultiplatformPublication` before
   and after and diff `engine/build/publications/kotlinMultiplatform/module.json` (expect no difference beyond
   timestamps/hashes of rebuilt files). Repeat for one Compose plugin, e.g. `:plugins:sprites`.
3. The apps still build: `./gradlew :app:ios:linkDebugFrameworkIosSimulatorArm64`,
   `./gradlew :app:web:wasmJsBrowserDistribution` (the `injectWebPreloads` patterns must still all match),
   `./gradlew :app:desktop:compileKotlin`, `./gradlew :app:android:assembleDebug`.
4. `./gradlew desktopTest`, then `./gradlew build`, recording its wall time next to the previous full build's.

## Manual check

Build and run the Showcase from Xcode on a simulator or device, and load the web distribution in a browser, to
confirm both still start.
