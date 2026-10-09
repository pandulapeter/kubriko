# Decide what to do with the undocumented public API in the engine's implementation package, then retire the PlatformUtils files

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** engine
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/InternalViewport.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoViewport.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformUtils.kt` → `Platform.kt`, `PlatformEffects.kt`, `LifecycleFocusEffect.kt` (new)
- `engine/src/{android,desktop,ios,web}Main/kotlin/com/pandulapeter/kubriko/implementation/PlatformUtils.<platform>.kt` → `Platform.<platform>.kt`, `PlatformEffects.<platform>.kt` (new)
- `engine/src/webMain/kotlin/com/pandulapeter/kubriko/implementation/BrowserDetection.kt` (new, option-dependent)
- `app/web/src/webMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt` (outside lane E, import only)
- `app/shared/src/webMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/Disclaimer.web.kt` (outside lane E, import only)
- `engine/CLAUDE.md`, `app/desktop/CLAUDE.md` (outside lane E), root `CLAUDE.md`

Depends on E51 (the `windowState` bug fix and KDoc) and E11 (comment conversions in the `PlatformUtils.*` actuals).

## Problem

Without explicit-API mode, three groups of declarations in `com.pandulapeter.kubriko.implementation` compiled public
by default and ship in the published `engine` artifact without KDoc:

1. `@Composable fun InternalViewport(modifier, kubriko, windowInsets)` (`InternalViewport.kt:42-43`, JVM facade
   `InternalViewportKt`) — only the public `KubrikoViewport` calls it (`KubrikoViewport.kt:31`).
2. `fun Window.isRunningOnAndroid()`, `isRunningOnIphone()`, `isRunningOnIpad()` (`PlatformUtils.web.kt:61-68`) —
   the Showcase's web shell uses the last two (`app/web/.../KubrikoShowcaseApp.kt:17`,
   `app/shared/.../Disclaimer.web.kt:12-13`); the engine itself uses none of them.
3. `lateinit var windowState: WindowState` (`PlatformUtils.desktop.kt:62`, facade `PlatformUtils_desktopKt`) — see
   E51; read by `plugin-pointer-input`, assigned by the Showcase.

None of them is used by Tesselar (grepped at 2480325f). Separately, `PlatformUtils.kt` is a `Utils` file by name
(`code-style`: "never `Utils.kt`"), holding unrelated things: `getPlatform()`/`getDefaultFocusDebounce()`, three
`Platform…Effect` Composables, and the shared `LifecycleFocusEffect`.

## Decision

1. `InternalViewport`:
   - **(a) Make it `internal`** — binary-breaking only for a consumer calling `InternalViewport` directly, which no
     known consumer does; needs a release allowed to break binary compatibility. Recommended for the next such
     release.
   - (b) Keep it public, add KDoc saying "use `KubrikoViewport`", mark it `@Deprecated(level = WARNING)` now and
     internal in the breaking release. **Recommended now** if no breaking release is planned.
2. `Window.isRunningOn…`:
   - **(a) Keep them public, document them, move them into `BrowserDetection.kt`** (web has no JVM facade; a klib
     resolves top-level functions by package and name, so a file move is not a break). Recommended — the Showcase
     needs them and other web games plausibly do too.
   - (b) Move them into `app/shared` (the only users) and delete them from the engine — a source break for any web
     consumer; breaking release only.
3. `windowState`: keep as is with E51's KDoc (recommended), or in a breaking release replace it with a parameter on
   `PointerInputManager.newInstance` (which E51 option (c) would make unnecessary).

## Fix (after the decision, recommended options)

1. `InternalViewport`: KDoc + `@Deprecated("Use KubrikoViewport.", ReplaceWith("KubrikoViewport(modifier, kubriko, windowInsets)", "com.pandulapeter.kubriko.KubrikoViewport"))`,
   with `@Suppress("DEPRECATION")` at the one call site in `KubrikoViewport.kt`.
2. Split the `PlatformUtils` files by what they hold, keeping every public declaration's binary name:
   - common: `Platform.kt` (`getDefaultFocusDebounce`, `getPlatform`), `PlatformEffects.kt` (`PlatformFocusEffect`,
     `PlatformFrameRateHint`, `PlatformMaximumDisplayRefreshRateEffect`), `LifecycleFocusEffect.kt`; all internal, so
     they move freely;
   - per platform: `Platform.<platform>.kt` and `PlatformEffects.<platform>.kt` with the matching actuals;
   - desktop: `windowState` is the only public declaration of `PlatformUtils.desktop.kt`. Either leave it alone in a
     file that keeps the name `PlatformUtils.desktop.kt` (simplest), or move it into `WindowState.desktop.kt` with
     `@file:JvmName("PlatformUtils_desktopKt")` — a single-file class needs no `@file:JvmMultifileClass` as long as no
     other file uses that name. Recommended: `WindowState.desktop.kt` with the `@file:JvmName`, and confirm with
     `javap -cp engine/build/classes/kotlin/desktop/main com.pandulapeter.kubriko.implementation.PlatformUtils_desktopKt`
     that `getWindowState`/`setWindowState` are still there;
   - web: the three `Window.isRunningOn…` functions to `BrowserDetection.kt` with KDoc.
3. Update the imports in the two Showcase files only if a package changes (it does not with the recommended options).
4. Grep the repo for `PlatformUtils` and fix every reference: `engine/CLAUDE.md` Key Internal Files bullet (as E09
   left it), `app/desktop/CLAUDE.md`, any skill.

## Behaviour
Unchanged at runtime.

## Public API
Per the decision. With the recommended options: `InternalViewport` gains KDoc and a deprecation (a warning for anyone
calling it); the `Window.isRunningOn…` functions gain KDoc; `windowState` keeps its facade. Nothing is removed.

## Tests
The existing ones; a `javap` check of `PlatformUtils_desktopKt` as above.

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:compileAndroidMain :engine:compileKotlinIosSimulatorArm64 :engine:compileKotlinWasmJs :app:web:compileKotlinWasmJs :app:desktop:compileKotlin :plugins:pointer-input:compileKotlinDesktop :engine:desktopTest`

## Manual check
none
