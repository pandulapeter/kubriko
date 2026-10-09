# Correct the stale statements in the engine and build-logic documentation

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** engine (KDoc and README); build-logic docs are unpublished
**Files:**
- `engine/README.md`
- `engine/CLAUDE.md`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/StateManager.kt`
- `engine/src/iosMain/kotlin/com/pandulapeter/kubriko/helpers/extensions/PointerIconExtensions.ios.kt`
- `gradle/build-logic/CLAUDE.md`

Runs after E05 (both edit `gradle/build-logic/CLAUDE.md`, different lines).

## Problem

Five statements no longer match the code at 2480325f:

1. `engine/README.md:25-26` — "the engine ensures that the internal set of Managers is unique by type (and the
   instances added last are kept)". `KubrikoImpl` keeps the **first** one: `addAll(manager.distinctBy { it::class })`
   (`KubrikoImpl.kt:44`), as root `CLAUDE.md`, `engine/CLAUDE.md` Gotchas and the `Kubriko.newInstance` KDoc
   ("the first one is used") already say.
2. `StateManager.kt` — the public KDoc of `isRunning` ends with
   `See the \`shouldPauseOnFocusLoss\` property of [ViewportFrameTickSource].` and the file imports
   `com.pandulapeter.kubriko.helpers.ViewportFrameTickSource` (:13) only for that link. `ViewportFrameTickSource` is
   `internal`, so the published documentation links to a type consumers cannot see; the public knob is the
   `shouldPauseOnFocusLoss` parameter of `TickSource.viewportFrames()`.
3. `PointerIconExtensions.ios.kt:14` — `// Can't implement invisible cursor because BrowserCursor is internal to
   Compose` was copied from the web actual; `BrowserCursor` has nothing to do with iOS (the web actual is in fact
   implemented, with `PointerIcon.fromKeyword("none")`).
4. `engine/CLAUDE.md` Key Internal Files, the `PlatformUtils.kt` bullet — lists `PlatformFocusEffect`,
   `PlatformFrameRateHint`, `getPlatform()`, `getDefaultFocusDebounce()` and "Android debounce = 350 ms; Desktop =
   0 ms", but the file also declares `PlatformMaximumDisplayRefreshRateEffect` and the shared `LifecycleFocusEffect`
   (used by the Android, desktop and iOS `PlatformFocusEffect`), and the iOS and web debounce are 0 ms as well.
5. `gradle/build-logic/CLAUDE.md:25` — "Desktop JVM (`jvm("desktop")`; JVM runs with `-XX:+UseZGC`)". Nothing in the
   repository sets that flag (grep for `ZGC` finds only this line).

## Fix

1. README: "…unique by type (and the instance passed first is kept)."
2. StateManager: replace the last KDoc line with
   `See the \`shouldPauseOnFocusLoss\` parameter of [TickSource.viewportFrames].` and replace the import with
   `import com.pandulapeter.kubriko.helpers.TickSource`.
3. iOS: delete the comment line, leaving `internal actual val pointerIconInvisible: PointerIcon = PointerIcon.Default`.
4. engine/CLAUDE.md bullet:
   "`PlatformUtils.kt` (+ actuals) — `PlatformFocusEffect` (Android, desktop and iOS delegate to the shared
   `LifecycleFocusEffect`), `PlatformFrameRateHint`, `PlatformMaximumDisplayRefreshRateEffect`, `getPlatform()`,
   `getDefaultFocusDebounce()`. Android debounce = 350 ms; desktop, iOS and web = 0 ms"
5. build-logic CLAUDE.md: "Desktop JVM (`jvm("desktop")`)".

## Behaviour
Unchanged (documentation and one import).

## Public API
None; only the KDoc of `StateManager.isRunning` changes.

## Tests
None (documentation).

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:compileKotlinIosSimulatorArm64`

## Manual check
none
