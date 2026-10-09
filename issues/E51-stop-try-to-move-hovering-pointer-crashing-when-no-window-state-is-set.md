# Stop PointerInputManager.tryToMoveHoveringPointer from crashing desktop apps that never set windowState

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** desktop (Windows, macOS)  ·  **Class:** Planned
**Artifact:** plugin-pointer-input (fix), engine (KDoc only)
**Challenged:** amended — option (a) also returns false while the window position is not yet determined (`WindowPosition.PlatformDefault`/`Aligned` have NaN coordinates, and `roundToInt()` throws on NaN), Wallbreaker is named as the second in-repo caller, and the `app/desktop/CLAUDE.md` edit is flagged as shared with lane A (A02, A17, A50 edit the same file).
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:**
- `plugins/pointer-input/src/desktopMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.desktop.kt` (outside lane E)
- `plugins/pointer-input/src/commonMain/kotlin/com/pandulapeter/kubriko/pointerInput/PointerInputManager.kt` (outside lane E, KDoc)
- `plugins/pointer-input/CLAUDE.md` (outside lane E)
- `plugins/pointer-input/README.md` (outside lane E)
- `engine/src/desktopMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformUtils.desktop.kt` (KDoc)
- `app/desktop/CLAUDE.md` (outside lane E, line 18)

Lane note: the code fix lives in `plugins/pointer-input`; whichever lane owns that module's files must not edit
`PlatformExtensions.desktop.kt` in the same sweep, or this plan moves to that lane. E54 (the decision about the
`implementation` package's accidental public API) builds on this plan.

## Problem

The engine exposes, without KDoc, a public top-level property in its `implementation` package
(`PlatformUtils.desktop.kt:62`):

```kotlin
lateinit var windowState: WindowState
```

`plugin-pointer-input` reads it whenever the public `PointerInputManager.tryToMoveHoveringPointer(offset)` runs on
Windows or macOS (`PlatformExtensions.desktop.kt:36-37`):

```kotlin
val x = (windowState.position.x.value + offset.x * densityMultiplier).roundToInt()
val y = (windowState.position.y.value + offset.y * densityMultiplier).roundToInt()
```

Only the Showcase assigns it (`app/desktop/.../KubrikoShowcaseApp.kt:55`, `windowState = rememberWindowState(...)`).
Nothing documents that a consumer must. A desktop game that calls `tryToMoveHoveringPointer` without having assigned
it — Tesselar, for one, declares its own local `val windowState = rememberWindowState()` in `Main.kt:20` and
`EditorApplication.kt:74` and never assigns the engine's — throws `UninitializedPropertyAccessException` from inside
its pointer handling, although the public KDoc promises "@return True if the pointer was moved, false otherwise".
(Tesselar does not call the function today; Space Squadron (`ShipDestination.kt`) and Wallbreaker (`Paddle.kt`) in this
repo do, and work only because the Showcase sets the property.)

`app/desktop/CLAUDE.md:18` also calls the property "an internal engine extension property"; it is neither internal
nor an extension.

## Fix

Options:
- **(a) Return false when the state is unset, in pointer-input — no API change. Recommended.** In
  `setPointerPosition` (desktop), after the Linux check:
  ```kotlin
  val windowPosition = try {
      windowState.position
  } catch (_: UninitializedPropertyAccessException) {
      return false
  }
  ```
  and compute `x`/`y` from `windowPosition`, after
  `if (!windowPosition.isSpecified) return false` — `WindowPosition.PlatformDefault` and `WindowPosition.Aligned`
  report `Dp.Unspecified` (NaN) coordinates until the window is shown, and `Float.roundToInt()` throws
  `IllegalArgumentException` on NaN, the same broken "returns false" promise by another path (rare: Compose Desktop
  replaces the position with an absolute one once the window is visible). (`::windowState.isInitialized` cannot be used: Kotlin allows it only
  where the `lateinit` property is lexically accessible, i.e. in the engine's own file.) Then document both ends:
  KDoc on `windowState` ("The state of the desktop window showing the game. Assign it from the `application { }`
  block, `windowState = rememberWindowState(...)`, for `PointerInputManager.tryToMoveHoveringPointer` to work; while
  it is unset that function returns false."), a sentence in the `tryToMoveHoveringPointer` KDoc ("On desktop it needs
  the engine's `windowState` to be set"), the same in `plugins/pointer-input/CLAUDE.md` "Cursor control" and
  `plugins/pointer-input/README.md` "Pointer Movement".
- **(b) An additive engine API** — `val isWindowStateInitialized: Boolean get() = ::windowState.isInitialized` in the
  same file (keeps facade `PlatformUtils_desktopKt`), checked by pointer-input. A new public symbol in the
  `implementation` package that E54 may then want to hide again; not recommended.
- **(c) Make the plugin independent of `windowState`** — derive the viewport's screen position from the plugin's own
  `onGloballyPositioned` coordinates (`LayoutCoordinates.localToScreen`, if Compose Desktop implements it for this
  version — verify first). This also fixes the offset that `windowState.position` (the window's outer corner, title bar
  included) introduces, but it changes where the cursor lands, so it is a behaviour change of its own for a later
  plan.

Fix `app/desktop/CLAUDE.md:18` (A02 and A17 already edited other paragraphs of the file, landed in d6cffee4 and
b864b37a; lane A's A50 still edits it, so merge word-level): "`windowState` is stored in a public top-level engine property
(`com.pandulapeter.kubriko.implementation.windowState`), which `plugin-pointer-input` reads to move the cursor."
(Check whether the Scene Editor still reads it — at 70de96c6 nothing in `tools/` does — and drop "like the Scene
Editor" if not.)

## Behaviour
With (a): a desktop app that never assigns `windowState` gets `false` from `tryToMoveHoveringPointer` instead of an
exception. Apps that assign it (the Showcase) behave exactly as before.

## Public API
(a): none — KDoc only. (b): one additive property. Retyping or removing `windowState` would break binary
compatibility (facade `PlatformUtils_desktopKt`) and is E54's decision, not this plan's.

## Tests
None feasible in pointer-input's desktopTest without a window: `setPointerPosition` drives `java.awt.Robot`. The
change is a three-line guard; review it by reading.

## Verify
`./gradlew :plugins:pointer-input:compileKotlinDesktop :engine:compileKotlinDesktop :app:desktop:compileKotlin`

## Manual check
Space Squadron and Wallbreaker on macOS or Windows still snap the cursor as before. A scratch desktop `main()` that never
assigns `windowState` and calls `tryToMoveHoveringPointer` gets `false` and keeps running.
