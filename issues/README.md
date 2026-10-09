# Fourth review sweep — platform edge cases

Reviewed commit: `401298a3` on `main` (clean tree). Angle: **platform edge cases** — underexercised platforms and
conditions (iOS lifecycle and hardware keyboards, the Wasm build and browser policies, Android configuration changes
and low-end devices, multi-monitor / high-refresh desktops, long sessions, suspended processes), with every
`expect` compared against all its `actual`s. Budget: standard — 5 area reviewers, 7 lane writers that verified every
finding at HEAD, 4 challengers.

43 reviewer findings → **44 plans** (the frame-rate finding split into 62 and 63; the docs-drift finding divided between 21 and 28). Nothing was dropped by
verification or by the challenge.

## Headlines

1. **`DisplayDivider` can lock to the wrong tick rate for good** (01) — one late frame at startup leaves
   `DisplayDivider(2)` at 12 fps instead of 30; moving a window from a 60 Hz to a 120 Hz screen gives 40 fps instead
   of 60. Proven by a failing probe test.
2. **Web music started before the first click never plays** (10), while `isPlaying` says it does.
3. **iOS hardware keyboards most likely deliver no keys at all** (21); **web maps Cmd, F-keys, −/=, numpad to
   Unknown** (22); **desktop gamepad slots swap players when a pad is unplugged** (20).
4. **iOS audio stops the user's own music as soon as a game composes** (11, decision).
5. **Desktop and web saves of different games share one store when they keep the default file name** (35, docs).
6. **Wallbreaker's ball tunnels through the paddle after one 45 ms frame** (60); the web Showcase sits on its loading
   bar forever on browsers without Wasm GC (50).

## Index

| # | Plan | Severity | Lane |
|---|---|---|---|
| 01 | Re-measure the display frame interval while the frame loop sleeps | medium | E |
| 02 | Restart the fixedFrequency timeline after the process was suspended | medium | E |
| 03 | Report the desktop refresh rate of the screen the window is on | medium | E |
| 04 | Stop detecting non-Apple touch browsers as iPhones and iPads | low | E |
| 05 | Combine the refresh-rate hints of every viewport in an Android window | low | E |
| 06 | Fall back to the default display when the Android context has none | low | E |
| 07 | Report 60 Hz on iPhones whose app does not unlock ProMotion | low | E |
| 10 | Resume web music that was started before the first user gesture | medium | U |
| 11 | Stop forcing a non-mixable Playback audio session on iOS | medium | U |
| 12 | Rewind a paused music track on stop and restart | low | U |
| 13 | Reset Android music looping for non-looping plays | low | U |
| 14 | Validate web sound effects at preload and catch rejected plays | low | U |
| 15 | Fail desktop sound loads that open no clip | low | U |
| 16 | Correct the iOS music stop documentation | low | U |
| 20 | Keep desktop gamepad slots sticky across SDL renumbering | medium | I |
| 21 | Read iOS hardware keys through GCKeyboard | medium | I |
| 22 | Map every web key code to Compose Key constants | medium | I |
| 23 | Report key releases only for keys reported as pressed | medium | I |
| 24 | Document which keys each platform backend hears | low | I |
| 25 | Keep the scroll-wheel zoom factor positive and symmetric | low | I |
| 26 | Account for window decorations when moving the desktop cursor | low | I |
| 27 | Only interpret web gamepads with the standard mapping | low | I |
| 28 | Correct the web pinch-zoom claim in the pointer-input docs | low | I |
| 35 | Document that persistence file names must be unique per game | medium | R |
| 36 | Make the sprite warm-up draw reach the GPU | low | R |
| 37 | Enable BlurShader on Android 12 | low | R |
| 38 | Document that sprites must not use density-qualified drawables | low | R |
| 40 | Ignore Escape released in another window in the scene editor | medium | T |
| 41 | Don't run scene undo or redo while a text field has focus | low | T |
| 42 | Keep the top inset out of the horizontal debug menu's log list | medium | T |
| 43 | Keep the debug menu toggle button clear of the status bar | low | T |
| 44 | Let the horizontal debug menu's left column scroll | low | T |
| 45 | Make the scene editor's file dialog filter on Windows and stop creating folders on load | low | T |
| 50 | Show an error on the web loading screen when the app cannot start | medium | S |
| 51 | Sync desktop fullscreen state when the OS enters fullscreen | low | S |
| 52 | Handle configuration changes in the Android Activity | low | S |
| 53 | Hide the home indicator and defer edge gestures in iOS fullscreen | low | S |
| 54 | Declare the touchscreen optional in the Android manifest | low | S |
| 60 | Cap the Wallbreaker ball's step so a long frame cannot tunnel through the paddle | medium | X |
| 61 | Make the physics demo explosion impulse independent of the tick rate | low | X |
| 62 | Scale Space Squadron alien fire chance by elapsed time | low | X |
| 63 | Make the Annoyed Penguins camera follow independent of the tick rate | low | X |
| 64 | Keep example shader and camera time continuous over long sessions | low | X |
| 65 | Return Annoyed Penguins to the menu when a level fails to load | low | X |

## Lanes

| Lane | Area (artifact) | Plans, in order | Files owned |
|---|---|---|---|
| E | engine (`engine`) | 01 → 07 | `engine/**`, `engine/CLAUDE.md`, `documentation/TICK_SOURCE.md`, root `CLAUDE.md` |
| U | audio (`plugin-audio-playback`) | 10 → 16 | `plugins/audio-playback/**` |
| I | input (`plugin-keyboard-input`, `plugin-gamepad-input`, `plugin-pointer-input`) | 20, 21, 22, 23, 24, 25, 26, 27, 28 | `plugins/keyboard-input/**`, `plugins/gamepad-input/**`, `plugins/pointer-input/**` |
| R | `plugin-persistence`, `plugin-sprites`, `plugin-shaders` | 35 → 38 | `plugins/persistence/**`, `plugins/sprites/**`, `plugins/shaders/**` |
| T | tools (`tool-debug-menu`, `tool-scene-editor`) | 40 → 45 | `tools/**` |
| X | examples (unpublished) | 60 → 65 | `examples/**` |
| S | Showcase app (unpublished) | 50 → 54 | `app/**` (incl. the Xcode project's Swift files) |

Order constraints inside lanes (numeric order satisfies all of them): E 03 → 05 (`PlatformEffects.desktop.kt`; 05
changes the internal expect signature), 05 → 06 (`PlatformEffects.android.kt`); U 11/12/16 share the iOS row of
`plugins/audio-playback/CLAUDE.md`, 12 → 13 share Android `play`; I 24 after 21 and 23, 28 after 25 (same CLAUDE.md
sections); T 40 → 41 (41 updates 40's test), 42 → 44 (`DebugMenuContents.kt`), 40 → 45 (`InternalSceneEditor.kt`);
S 52 → 54 (`AndroidManifest.xml`, `app/android/CLAUDE.md`).

**Merge order: E → U → I → R → T → X → S.** The engine first, since plan 05 changes an internal expect every
platform builds on; the plugins next in dependency-free order; tools after the input plugins (40's Escape fix is
written to work with every option of 23); examples, then the app last (52 must see 05 landed).

**Shared files.** Only lane E edits root `CLAUDE.md` (02: TickSource table row; 07: `maximumDisplayRefreshRate`
bullet) and `documentation/TICK_SOURCE.md` (02). Every other lane touches only its own module `CLAUDE.md`/`README.md`
files. No plan edits a `strings.xml` (50's messages are static HTML outside Compose, like the existing
`alt="Loading..."`). No conflicts are expected when cherry-picking.

## Decisions

**Answered 2026-10-09: the recommended option for all 14** (02, 11 and 23 asked individually; the rest as a
batch). Plans that change published behaviour are marked **API** — list them in the next release notes.

| Plan | Question | Answer (recommended) | Alternatives not taken |
|---|---|---|---|
| 02 **API** | `fixedFrequency` after a gap > interval + 2 s | Re-anchor and emit one tick with delta 0 (like the first tick); KDoc + TICK_SOURCE.md | No tick for that wake-up · clamp the delta · document only |
| 04 **API** | Public `isRunningOnIphone`/`isRunningOnIpad` misclassify Firefox Android / touch Windows | Fix in place (results change only where wrong today) | Fix in app/ only · fix, deprecate and move to the Showcase |
| 07 **API** | iPhone without `CADisableMinimumFrameDurationOnPhone` reports 120 Hz it can't reach | Report 60 there + document the key in KDoc | Document only · report 60 + one-time log |
| 11 **API** | iOS audio session category | Ambient, only while the session is still at the system default (SoloAmbient); no API change | Playback + MixWithOthers · new parameter · keep |
| 12 **API** | `play(shouldRestart = true)` on a paused track | Always rewind (Desktop already does); KDoc updated | Rewind only a playing track and change Desktop |
| 23 **API** | When `onKeyReleased` fires | Only for keys previously reported pressed | Gate releases on focus · document only |
| 24 | Keyboard sources differ per platform | Document it | Route all through a Compose key modifier (not recommended) · intercept Android `Window.Callback` |
| 27 **API** | Web pads without the "standard" mapping | Treat as not connected (what the docs already claim) | Expose only the left stick · docs only |
| 35 | Default persistence `fileName` shared on desktop/web | Keep the default, document uniqueness (and no `/`, ≤ 80 chars) | Make it required (source break) · derive a default (orphans saves) |
| 37 **API** | BlurShader on Android 12/12L | Enable from API 31 (what its KDoc promises) | Keep API 33 and document |
| 43 **API** | Public `DebugMenu.OverlayOnly` and window insets | Leave unchanged (only `invoke` is fixed) | Additive overload with `windowInsets` in -api/-noop · apply `safeDrawing` itself (not recommended) |
| 52 | Android `configChanges` list | `orientation\|screenSize\|screenLayout\|smallestScreenSize\|keyboard\|keyboardHidden\|navigation\|density` | Only `keyboard\|keyboardHidden\|navigation` · also `uiMode` |
| 53 | Edges deferred in iOS fullscreen | `.all` (matches Android's transient bars) | `.bottom` only |
| 54 | Also declare `faketouch` optional | No | Yes |

## Challenge

Four fresh challengers (E; U+R; I+T; S+X) tried to break every fix: **30 sound, 14 amended, 0 dropped**; no
recommendation changed. Notable catches:
- 15: on a machine with no audio device the fix would have re-decoded and re-logged every sound on every `play()` —
  now keeps today's silent empty sound when no `Clip` line exists at all.
- 25: Float `exp` underflows to 0 for large deltas, so the fix failed its own test — the exponent is clamped to ±10.
- 07: the plist flag is an `NSNumber`; a failing `as? Boolean` would have capped every iPhone, Showcase included.
- 20: polling must hold the shared `JamepadRuntime` lock — Jamepad reopens every handle on hot-plug, and another
  instance's background TickSource could touch a closed handle (native crash).
- 50: the dev-server path returns early before the new code would run, and the benign ResizeObserver notice would
  have shown the error screen.
- 05: reading the viewport size in composition would have recomposed `InternalViewport` on every resize.
- Also amended: 04 (keep the `!Chrome` check), 10 (listener capture flag, caught `resume()` rejections), 14 (sound
  URIs must be fetchable — documented), 21 (copy-on-write listeners, main-thread registration), 28 (trackpad pinch
  may also zoom the page — manual check), 35 (name limits), 38 (any non-density qualifier is fine), 51
  (requested-placement guard against stale reports during the macOS exit animation).

## Checked and found solid

Engine: the Wasm triangle bridge against Skiko 0.150.1 (arity, order, heap views, pagehide/bfcache); `drawTriangles`
actuals; the Android texture probe; minSdk 29 API guards; `viewportFrames()` long-gap handling and per-platform focus
handling; monotonic time sources; `getPlatform()` and `PointerIcon.Invisible` actuals. Plugins: `AudioCache`,
Android SoundPool, desktop music jobs; Android/iOS gamepads; keyboard focus-loss flush, web blur, iOS key codes;
pointer cancellation; persistence error handling on every platform; kotlinx-JSON serialization (no locale);
shader fallbacks below API 33; the physics accumulator cap. Tools: ui-components expect/actuals, Logger, locale-free
scene-editor number formatting, noop surfaces. App: `ShowcaseSession` across recreation, back handling, edge-to-edge,
iOS ProMotion plist key and orientations, desktop title bar and prefs storage, web progress wrapper and fullscreen
sync, the publish workflows. Examples: focus-loss pause in every game, music following focus, delta-scaled movement,
touch/keyboard fallbacks, shaders-unsupported messages, no locale formatting.

## Dropped after verification

None.

## Noted, not planned

- Tesselar passes `PersistenceManager.newInstance(fileName = "preferences")`, a generic name plan 35's docs warn
  against — worth changing there (outside this repository).
- The debug menu's log-filter `TextInput` inside the scene editor does not report focus to the editor, so T/S/R,
  arrows and undo still reach the editor while typing there.
- `plugins/shaders/README.md` snippets don't match the API (`BlurShader(radius = 10f)` etc.); plan 37 fixes them
  only opportunistically.
- Foldables: the Android frame-rate hint is not re-picked when the display mode set changes (52's manual check);
  if it fails, the follow-up must re-apply 05's combined per-window request.

## Manual checks owed

Every plan's **Manual check** section. By platform:
- **iOS device:** 07, 11, 12, 16, 21 (iPad + hardware keyboard), 42, 43, 53.
- **Android device:** 05, 06 (DreamService host), 12, 13, 37 (API 31 emulator), 42, 43, 44 (font scale 1.3/2.0),
  52 (low-end device, BT controller, rotation, foldable), 54 (Play Console device catalog).
- **Browser:** 04 (Firefox Android), 10, 14, 22, 25, 27, 28 (Mac trackpad), 50 (Safari 17 / iOS 17, offline,
  dev server, extensions), 60–65 under CPU throttling.
- **Desktop:** 01 (isometric demo idle), 03 (two monitors), 15, 20 (two pads, unplug the first), 26 (decorated
  window), 40, 41, 45 (Windows), 51 (macOS green button, quick toggles).
- **Showcase play-throughs:** 36 (first-use hitch), 60, 61, 62, 63, 64, 65 (block a level JSON in devtools).
