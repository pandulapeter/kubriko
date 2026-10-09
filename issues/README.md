# Third review sweep — structure, SOLID, readability, testability

Reviewed at `2480325f` on `main` (2026-10-09). Angle: architecture and internal code quality — SOLID, readability,
developer satisfaction, testability, Compose best practices; in the Showcase app every non-private top-level Composable
in a file of its own. Ground rules from the user: no public API change unless absolutely necessary; behaviour changes
only where they fix a bug; **do the simple refactors now, plan the complex ones** (the `codebase-review` skill's
section 5). The user had uncommitted edits to `README.md` and the iOS xcschemes when the sweep started; no plan touches
them.

**Status (2026-10-09):** all 179 Now plans landed on `main` as `92b16c51..70de96c6` (on top of the plans commit
`91c5941b`; 182 commits incl. three small follow-ups), unit tests and the full build green. The 48 Planned plans below
were rebased on `70de96c6` (each carries a `**Rebased:**` line) and wait for the decisions.

Process: six read-only area reviewers (engine + build-logic, plugins, tools, app, games, demos/tests) → six lane writers
that re-verified every finding at `2480325f` and wrote the plans → four fresh challengers. **227 plans: 179 Now
(`<lane>01`–`49`), 48 Planned (`<lane>50`+).**

## Headlines

- **Bugs fixed now:** a scene-editor save of the physics demo's `DynamicChain` grows the chain by a link and shifts it on
  every save/undo (D15); the Showcase's loading gate misses the isometric demo's title and a licence name (A12); the
  scene editor's body inspector picks "the first `PointBody`-typed member" by reflection (T18); the test-input keyboard's
  left Shift is narrow (D37) and its gamepad strings aren't preloaded (D39); the isometric joystick ignores the host's
  insets (D46); Annoyed Penguins logs a manager under the wrong tag (G31).
- **Bugs that need a decision:** desktop `PointerInputManager.tryToMoveHoveringPointer` throws in any app that never set
  the engine's public `windowState` — Tesselar included (E51); `RaycastExplosion` never casts a ray, so it does nothing
  (P50); Space Squadron's own death explosion scores a point (G58); Wallbreaker's top-right corner bounce looks wrong
  (in G59).
- **The big structural plans:** split `ActorManagerImpl` (E50), extract the frame throttle into a tested
  `FrameTickScheduler` (E52), inject dispatchers/clock for virtual-time tests (E53), a shared Skia source set (E55),
  split the scene editor's `EditorController` (T53), a Showcase session object instead of globals (A51), a
  scene-editor connection instead of global flags (G55), an info-panel `CompositionLocal` (G60).
- **Now:** 179 local refactors — file-per-declaration splits (facades kept with `@file:JvmName` +
  `@file:JvmMultifileClass` where a published file splits), Composable extractions, dead code, KDoc for undocumented
  public API, stale `CLAUDE.md` statements, tests for pure logic that had none.

## Index of the remaining (Planned) plans

✱ = amended by the challenge. The Now plans' rows were removed when they landed; `git log 91c5941b..70de96c6` lists them.

| Plan | Fix | Kind | Severity | Class |
|---|---|---|---|---|
| [E50](E50-split-actor-manager-impl-into-batch-processor-culler-and-layers.md) | Split ActorManagerImpl into a batch processor, a culler with the draw caches, and the layer Composables ✱ | refactor | high | Planned |
| [E51](E51-stop-try-to-move-hovering-pointer-crashing-when-no-window-state-is-set.md) | Stop PointerInputManager.tryToMoveHoveringPointer from crashing desktop apps that never set windowState ✱ | bug | medium | Planned |
| [E52](E52-extract-the-frame-throttle-into-a-testable-frame-tick-scheduler.md) | Extract the viewport's frame-throttle algorithm into an internal FrameTickScheduler with synthetic-timestamp tests | refactor | high | Planned |
| [E53](E53-inject-the-engine-dispatcher-and-clock-so-tests-can-use-virtual-time.md) | Let the engine's coroutine context and clocks be injected internally, so tests run on virtual time ✱ | test | medium | Planned |
| [E54](E54-decide-the-fate-of-the-accidental-public-api-in-the-implementation-package.md) | Decide what to do with the undocumented public API in the engine's implementation package, then retire the PlatformUtils files | refactor | medium | Planned |
| [E55](E55-give-the-skia-targets-a-shared-skiko-source-set.md) | Give the Skia-backed targets (desktop, iOS, Wasm) a shared skikoMain source set so their identical actuals exist once ✱ | build | medium | Planned |
| [P50](P50-make-ray-scatter-cast-its-rays.md) | Make `RayScatter.castRays` actually cast `noOfRays` evenly rotated rays, so `RaycastExplosion` affects bodies | bug | medium | Planned |
| [P51](P51-extract-the-sweep-and-prune-broad-phase-into-its-own-class.md) | Extract the sweep-and-prune broad phase from `PhysicsManagerImpl` into an internal class with a direct pair-order test | refactor | medium | Planned |
| [P52](P52-pin-the-collision-raycast-results-with-tests.md) | Pin plugin-collision's raycast results with tests | test | medium | Planned |
| [P53](P53-share-one-polygon-entry-edge-scan-between-the-raycast-variants.md) | Share one polygon entry-edge scan between `raycastPolygonEntryDistance` and `raycastPolygonHit` | refactor | low | Planned |
| [P54](P54-make-the-keyboard-input-manager-testable-and-test-its-latch.md) | Make `KeyboardInputManagerImpl` reachable from tests without composition and test its one-tick latch | test | medium | Planned |
| [P55](P55-make-the-gamepad-input-manager-testable-and-test-its-state-derivation.md) | Let tests hand `GamepadInputManagerImpl` a fake event handler and test its dead zone, triggers and button diff | test | medium | Planned |
| [P56](P56-extract-gamepad-focus-navigation-into-its-own-class.md) | Extract the gamepad focus-navigation loop from `GamepadInputManagerImpl` into an internal `GamepadFocusNavigator` | refactor | low | Planned |
| [T50](T50-take-the-selection-side-effects-out-of-state-flow-update.md) | Run removeSelectedActor's side effects outside StateFlow.update and read the selection from its source | bug | low | Planned |
| [T51](T51-let-the-editor-controller-own-the-placement-preview-actor.md) | Let EditorController derive and own the placement preview actor instead of OverlayManager assigning it | refactor | medium | Planned |
| [T52](T52-scope-the-scene-editor-drag-state-to-the-editor-instance.md) | Replace the file-level drag state globals in ModifierExtensions.kt with a per-editor ActorDragState and pure drag math | refactor | low | Planned |
| [T53](T53-split-editor-controller-into-focused-collaborators.md) | Split EditorController into a scene document, scene file I/O, a camera animator and a selection holder, with injected scope and clocks ✱ | refactor | medium | Planned |
| [T54](T54-decide-the-fate-of-the-public-float-to-difference.md) | Decide what to do with the accidentally public Float.toDifference() in tool-scene-editor | refactor | low | Planned |
| [T55](T55-decide-what-is-scene-editor-available-means.md) | Decide what IS_SCENE_EDITOR_AVAILABLE means, since both the real and the noop module declare it false | docs | low | Planned |
| [T56](T56-zero-pad-the-debug-menu-log-timestamps.md) | Zero-pad the time in debug menu log entries | bug | low | Planned |
| [G50](G50-share-the-duplicated-game-ui-helpers-through-examples-shared.md) | Share the duplicated game UI helpers (hover tracking, font typography) through `examples/shared`. | refactor | low | Planned |
| [G51](G51-make-each-games-back-navigation-a-tested-pure-function.md) | Turn each game's `navigateBack` decision tree into a pure, unit-tested function. | test | medium | Planned |
| [G52](G52-give-game-ui-events-intention-named-manager-methods.md) | Move each game's UI-event handling and click sounds out of the entry Composables into intention-named manager methods. | refactor | low | Planned |
| [G53](G53-stop-writing-loading-state-during-composition-in-the-games.md) | Stop writing loading state during composition in the games' `LoadingManager`s. | refactor | medium | Planned |
| [G54](G54-give-each-game-entry-composable-a-single-root-and-drop-the-redundant-space-squadron-animated-visibility.md) | Give each game's entry Composable a single root that takes `modifier`, and drop Space Squadron's redundant nested `AnimatedVisibility`. | refactor | low | Planned |
| [G55](G55-replace-the-global-scene-editor-visibility-flags-with-a-scene-editor-connection.md) | Replace the module-global scene-editor flags of the games and demos with a `SceneEditorConnection` owned by the state holder. ✱ | refactor | medium | Planned |
| [G56](G56-give-annoyed-penguins-levels-a-type-with-localised-names.md) | Give Annoyed Penguins levels a type with localised display names instead of using "Map 1/2/3" map keys as labels. ✱ | refactor | low | Planned |
| [G57](G57-decide-what-to-do-with-the-unused-input-managers-in-annoyed-penguins-and-blockys-journey.md) | Decide what to do with the registered-but-unused input managers in Annoyed Penguins and Blocky's Journey. | refactor | low | Planned |
| [G58](G58-stop-the-space-squadron-ship-explosion-from-scoring-a-point.md) | Stop Space Squadron's own ship explosion from scoring a point, and stop the ship pushing HUD state into `UIManager`. ✱ | bug | low | Planned |
| [G59](G59-make-the-wallbreaker-ball-bounce-a-tested-pure-function.md) | Make the Wallbreaker ball bounce a pure, tested function (and decide the top-right corner case). | test | medium | Planned |
| [G60](G60-provide-the-showcase-info-panel-visibility-through-a-composition-local.md) | Provide the Showcase info-panel visibility through a `CompositionLocal` instead of the global `StateHolder.isInfoPanelVisible`. ✱ | refactor | medium | Planned |
| [D50](D50-make-dynamic-chains-link-count-edit-rebuild-the-chain.md) | Make editing `DynamicChain`'s `linkCount` in the scene editor rebuild the whole chain, and settle what `linkCount` counts. | bug | low | Planned |
| [D51](D51-give-the-time-driven-shaders-one-base-and-drop-the-controls-casts.md) | Give the five time-driven animation shaders one shared base and map each demo type to its code and controls without unchecked casts. ✱ | refactor | low | Planned |
| [D52](D52-implement-or-remove-test-audios-loop-button.md) | Decide whether test-audio's permanently disabled loop button gets implemented or removed with its dead resources. ✱ | bug | low | Planned |
| [D53](D53-check-and-drop-test-audios-extra-viewport-insets-padding.md) | Check whether test-audio's extra `windowInsetsPadding` on the `KubrikoViewport` is needed, and drop it if not. | refactor | low | Planned |
| [D54](D54-re-enable-or-delete-the-collision-tests-ray-emitter.md) | Decide whether test-collision's unused `RayEmitter` is re-enabled (with real ray casts) or deleted, and remove the commented-out code either way. | refactor | low | Planned |
| [D55](D55-publish-the-joystick-layout-as-one-immutable-value.md) | Replace the seven mutable layout `var`s the UI pushes into the isometric `ControlOverlayManager` with one immutable joystick layout and a single geometry function. | refactor | low | Planned |
| [D56](D56-split-the-isometric-renderer-files-holding-several-types.md) | Split the isometric renderer files that hold several top-level types, after doing it in Tesselar first. | refactor | low | Planned |
| [D57](D57-share-one-scatter-loop-in-the-isometric-logic-manager.md) | Share one rejection-sampling scatter loop for the isometric demo's NPCs, trees and bushes, after doing it in Tesselar first. | refactor | low | Planned |
| [D58](D58-share-the-expandable-controls-overlay-in-examples-shared.md) | Share the "info panel + expandable bottom-end controls panel + brush button" overlay through `examples/shared`. | refactor | medium | Planned |
| [D59](D59-share-the-shaders-not-supported-fallback.md) | Share the "shaders not supported" fallback and its string between the two shader demos. | refactor | low | Planned |
| [D60](D60-share-the-delayed-loading-overlay-dismissal.md) | Share the "hide the loading overlay 300 ms after the actors arrive" logic between the demos that repeat it. | refactor | low | Planned |
| [A50](A50-give-the-desktop-window-and-its-fullscreen-toggle-their-own-files.md) | Give the desktop window and its fullscreen toggle their own files instead of a local Composable over six loose states in `main()` | refactor | medium | Planned |
| [A51](A51-replace-the-showcase-globals-with-a-session-passed-from-the-root.md) | Replace the file-level selection and state holder globals with one process-scoped `ShowcaseSession` passed down from the root | refactor | medium | Planned |
| [A52](A52-hoist-the-welcome-screen-more-info-state-to-its-callers.md) | Hoist the Welcome screen's "more info" state to its callers and dissolve the namespace-only `WelcomeScreenStateHolder` | refactor | low | Planned |
| [A53](A53-follow-the-live-theme-for-the-showcase-surface-elevations.md) | Follow the theme `KubrikoTheme` is actually in for the Showcase's surface elevations, and define them once | bug | low | Planned |
| [A54](A54-list-the-missing-runtime-dependencies-on-the-licenses-screen.md) | List the runtime dependencies the Licenses screen misses (Jamepad and its SDL2 natives, `jbr-api`, NavigationEvent) | bug | medium | Planned |
| [A55](A55-decide-whether-dependency-names-need-string-resources.md) | Decide whether proper names (the Licenses screen's dependency names) are exempt from the string resource rule | docs | low | Planned |

## Lanes

| Lane | Area | Planned |
|---|---|---|
| E | `engine/`, `gradle/build-logic/`, root `CLAUDE.md`, `documentation/` | E50–E55 |
| P | `plugins/*` | P50–P56 |
| T | `tools/*` | T50–T56 |
| G | `examples/shared`, `examples/game-*` | G50–G60 |
| D | `examples/demo-*`, `examples/test-*` (incl. `-noop`) | D50–D60 |
| A | `app/*` | A50–A55 |

The Now lanes ran in parallel from `91c5941b` and merged E → P → T → G → D → A without a conflict. Dependencies among
the Planned plans (each plan states its own): E50 → E53; E51 → E54; P52 → P53; P55 → P56; T50, T51 → T53; A51 → A52,
G55, G60 (decide those four together); D58 → D59.

Shared-file rules: a lane edits only its own modules' `CLAUDE.md` files; only lane E edits the root `CLAUDE.md` and
`documentation/`. Planned plans cross lanes in places — E51/E54 touch `plugins/pointer-input` and
`app/desktop/CLAUDE.md` (also edited by A02/A17/A50); E55 moves shaders/sprites actuals (after P03/P10/P14); G55 and
G60 reach into demo-performance/demo-physics, the demo overlays and `app/`; E50 edits `examples/demo-physics/CLAUDE.md`
(locate by text, D22 edits it first). Schedule those after the Now lanes merge.

## Decisions

**Answered 2026-10-09: the recommended option for every plan below** (P50 fix, E51 return `false`, G58 remove the
death-explosion point asked individually; the rest accepted as a batch). Do not re-ask.

- **E50** split `ActorManagerImpl`: unify the two viewport-bounds tests as one helper with identical arithmetic, as a
  second commit (vs keep both / reuse `isWithinViewportBounds`, which can flip an edge actor); keep the name `Layers`.
- **E51** `windowState` crash: pointer-input returns `false` when `windowState` is unset or its position unspecified (no
  API change; behaviour goes from throwing to `false`) — vs an additive public `isWindowStateInitialized` — vs computing
  the position from `localToScreen` later.
- **E52** `FrameTickScheduler` is a pure class fed time and gate state (vs one holding the managers).
- **E53** inject an internal dispatcher (wrapped in `SupervisorJob`) and a primitive `TickClock` fun interface (not
  `TimeSource`, which would allocate per tick).
- **E54** accidental public API in `implementation`: `InternalViewport` gets KDoc + `@Deprecated(WARNING)`, internal in
  the next binary-breaking release; `Window.isRunningOn…` documented and moved to `BrowserDetection.kt`; `windowState`
  kept in `WindowState.desktop.kt` with the old facade name.
- **E55** shared source set named `skikoMain`; web keeps its bridge through an expect hook.
- **P50** `RaycastExplosion` starts applying impulses (fix, note it in the release notes) — vs document as
  non-functional — vs deprecate.
- **P53** share the raycast edge scan by returning the winning edge index (one extra edge evaluation per hit mask).
- **P56** also needs a call on the private `moveFocus` and on `isAnyGamepadPressing`, which both `onUpdate` and the focus loop use (found while rebasing).
- **P55** gamepad test seam as an internal constructor parameter; **P56** extract `GamepadFocusNavigator` after P55.
- **T51** the placement preview keeps following the mouse after a placement (today's behaviour).
- **T54** public `Float.toDifference()`: KDoc + `@Deprecated(WARNING)` now, internal in the next breaking release.
- **T55** `IS_SCENE_EDITOR_AVAILABLE`: document it as a placeholder (vs make it `true` in the real module, vs deprecate).
- **T56** zero-pad the debug menu's log timestamps (`HH:mm:ss.SSS`).
- **A51** Showcase session: create the selected holder in `select()`, pass the session as parameters.
- **A52** the welcome screen's "more info" state lives in the A51 session.
- **A53** elevations: app-only fix reading darkness from the surface colour, after a manual macOS live-theme check.
- **A54** list Jamepad (+ SDL2), `jbr-api` and NavigationEvent unconditionally, after checking each licence.
- **A55** exempt proper names (dependency names) from the strings rule in `code-style`.
- **G50** share only the hover modifier and typography helpers in `examples/shared`.
- **G51** one pure `navigateBack` function per game; the drift between games becomes separate bug plans later.
- **G52** the UI managers own the click sound; **G53** minimal `SideEffect`/`LaunchedEffect` fix.
- **G54** one root `Box(modifier)` per game entry + a manual comparison of each game.
- **G55** the process-scoped pool (A08 / A51) owns one scene-editor connection per entry — decide with A51.
- **G56** a parameterized `Map %1$d` level name; **G57** drop Annoyed Penguins' unused keyboard manager, keep Blocky's
  Journey's managers and fix its info text; **G58** remove the death-explosion point, expose ship state as flows;
  **G59** extract the bounce first, then fix the top-right corner (`+1, -1`) as its own bug commit.
- **G60** info-panel visibility stays process-scoped (A51 session); only the reads move to a `CompositionLocal`.
- **D50** `DynamicChain.linkCount`: drop `@Exposed` from the setter and keep "linkCount + 1 links".
- **D51** shader controls bound by typed lambdas built in the state holder; **D52** implement the loop button;
  **D53** remove the extra padding only if a device check shows no change; **D54** delete `RayEmitter`; **D55**
  `JoystickLayout` data class; **D56/D57** leave the Tesselar-synced renderer and `LogicManager` until the next sync;
  **D58/D59/D60** give `examples/shared` its own compose resources and a shared helper for performance/physics.

## Challenge

Four fresh challengers (E+P, T+A, G, D) tried to break every fix against `2480325f`: **197 sound, 30 amended, 0
dropped, 0 reclassified.** Notable catches: `kotlin.jvm` isn't default-imported in `commonMain`, so the facade-keeping
splits import `JvmName`/`JvmMultifileClass` (P04, T23); E50's culler would have made the public actor flows throw before
init; E53's `TimeSource` clock would allocate per tick; T12's alpha assertion ignored 8-bit channels; D30 would have
decided play/pause from live state instead of the drawn icon; G55/G60's `remember`-based options would reset on
Android configuration changes and the Windows fullscreen toggle; three Verify task names were mangled (D28/D31/D34).

## Checked and found solid

Engine: Manager lifecycle and delegates, `KubrikoImpl` lookup/init/tick dispatch, `SyncStateFlow`, State/Metadata
managers, body AABBs, `TriangleBatch`, `WasmTriangleBridge`, coordinate conversions, build-logic convention plugins.
Plugins: the `sealed class` + `Impl` + `newInstance` pattern, `AudioCache`, `SpriteManagerImpl`, `SpatialHashGrid`,
`PersistedPropertyWrapper`, `CollisionManagerImpl`/`Arbiter`. Tools: the tested scene-editor helpers,
`LocalTextInputFocusReporter`, `RefCountedRegistry`, Logger, real/noop/api splits. App: strings, platform entry points,
`ReviewButton`/`Disclaimer` expect/actuals. Examples: `GameButton`/`gameRipple`, the `StateHolder` contract, per-frame
code, manager sharing between instances, -noop mirroring.

## Dropped after verification

None. Merged: the engine's and plugins' identical Skia actuals → E55; the games' and demos' scene-editor connection →
G55; the games' and demos' info-panel global → G60. Folded: moving `isSceneEditorVisible` into its own file → G55.
Not raised (out of angle): `ParticleManagerImpl.onUpdate` and Annoyed Penguins' `Slingshot.activePenguin` allocate per
tick (performance).

## Landed

All 179 Now plans, one commit each, `92b16c51..70de96c6`; none skipped. Follow-ups committed by the orchestrator:
`f4dd8a76` (the review skill's attribution grep no longer flags messages naming a `CLAUDE.md`), `a84a8dae` (ui-components
guide: only Android and iOS can share text), `12a108fa` (physics guide states the sub-step sync constraint without its
history). Deviations worth knowing: three facade-keeping splits are the repo's first `@file:JvmMultifileClass`
(`GamepadFocusNavigationKt`, `SmallSliderKt` — checked with `javap`/the jar); `tool-logger`'s published POM loses
`kotlinx-datetime` (T25 — mention in the release notes); several split files gained a trailing newline.

## Manual checks owed

Each plan's "Manual check" section; the ones that matter most: the scene editor (T08–T18, D15 save/undo of the physics
chain), the debug menu (T19–T22), the Showcase on desktop (A02, A10, A13, A14), web (A15) and Android, every game and demo
screen after its overlay extraction (G14–G27, D02–D47), the isometric demo's joystick in the wide windowed layout (D46).
