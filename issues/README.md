# First review sweep — stability & correctness

**Reviewed commit:** `0008d027` on `main` (clean working tree). **Date:** 2026-09-28.
**Angle:** stability & correctness — lifecycle and ordering, threading, dispose/leaks, focus/pause, shared Managers,
numeric edge cases, contract vs. behaviour. Per-frame allocation work was out of scope (three earlier passes covered it).
**How it was done:** five read-only area reviewers (engine, plugins, tools, Showcase app, examples) plus one headless
live stress run of the engine and the collision/physics plugins, then one writer per lane that re-verified every
finding against `0008d027` (pure logic proven with throwaway probe tests, since deleted) and wrote the plans.

92 plans: `00` (test setup, lands first) plus 91 fixes in six lanes. 35 carry a **Decision needed** line.

**Challenge pass (done 2026-09-28).** After the user found a flaw in plan 05's fix, five fresh challengers tried to
break every plan: each fix was traced through the documented usage (`documentation/*.md`, `CLAUDE.md`, KDoc), every
in-repo caller, every other plan touching the same behaviour, the threading the fix itself introduces, and
Tesselar. Result: **60 sound, 31 amended, 0 dropped, 0 new decisions**, and one new plan (`99`). Each plan's
`**Challenged:**` line says what changed. The amendments that matter most:
- `05`: the actor operations queued before `start()` are applied synchronously inside it, repeating until
  nothing is left (cascading adds), so the documented headless `start(); tick()` flow is deterministic.
- `07`: the headless first tick updates the actors (it was lost as written). `06`: no double cull.
- `13`: "not focused" is reported only when the *last* viewport of an instance leaves composition. Its
  Annoyed Penguins side effect (pausing on every level load) is fixed by the new plan `99`.
- `20`: a companion edit to Annoyed Penguins' `GameplayManager.onUpdate`, so the level-end fade doesn't flash.
- `33`/`34`/`38`: one atomic audio slot map. A failed sound stays "settled" while it retries, because otherwise every
  play of a broken sound would swap the game for the loading screen.
- `00`: the Wasm and iOS test run tasks are disabled, so `./gradlew build` needs no browser or simulator.
- Tests that would have passed at HEAD, or only after a later plan (`03`, `05`, `16`, `17`, `51`), were fixed or
  labelled; `57` uses a fake `ImageBitmap`, since library test classpaths have no native Skia runtime.
- `02`/`03` document that actor identity is `equals`-based (data-class actors that compare equal count as one).

## Headlines

1. **Data loss — iOS persistence never keeps a `string()`/`generic()` value** (`45`). `putString` writes the bare
   key, `getString` reads the prefixed one, so every saved string is back to its default after a relaunch.
2. **Data loss — the scene editor destroys work**: Escape with nothing selected closes it and throws away an unsaved
   scene (`70`); opening an unreadable file silently empties the scene and resets undo, and Save then writes `[]`
   over that file (`71`).
3. **ActorManager loses or corrupts the scene under ordinary use** (`01`–`07`, measured by the stress run): one
   throwing `onAdded` silently drops the whole batch; adding a present actor duplicates it (seen up to 10×, updated
   twice per tick); add-then-remove in one batch fires `onRemoved`/`dispose` without `onAdded` (500× in a churn run);
   a Group cycle hangs the processor and eats 1.6 GB in 2.5 s; every removed actor gets one more `update()` after
   `dispose()` (30/30 runs); headless instances never update any `Dynamic` actor.
4. **Crashes on real devices**: Android sound loads crash with `ConcurrentModificationException` or play the wrong
   sample (`32`); `generic()` persistence crashes on first launch (`46`); the Showcase's Share crashes on iPad (`77`);
   any link crashes on an Android device without a matching app (`82`); racing `TickSource.start()`/`stop()` leaks tick
   loops that run concurrently (`10`).
5. **Release trap for consumers — the noop debug menu renders a blank screen** (`65`): its `invoke` never draws the
   viewport it wraps, contrary to its README.
6. **macOS Showcase regression** (`80`): toggling full screen permanently removes the title-bar drag and
   double-click-zoom listener (undoing `65bd769e`) and resets every viewport.
7. **One bad value degrades a whole plugin**: one NaN body makes physics miss 40–50% of contacts (`51`); one NaN or
   infinite collision mask makes `CollisionManager` 100× slower (`54`).

## Index

| # | Plan | Sev | Lane |
|---|---|---|---|
| 00 | Add a unit test setup to the library modules | high | pre-lane |
| 01 | Isolate actor callback failures so a batch always completes and is published | high | A |
| 02 | Guard Group flattening against cycles and repeated members | high | A |
| 03 | Ignore additions of actors that are already in the scene | high | A |
| 04 | Pair actor lifecycle callbacks when one batch both adds and removes an actor | medium | A |
| 05 | Process actor operations only after every Manager is initialized; apply pre-start ones synchronously | high | A |
| 06 | Stop updating and drawing actors once their removal has been applied | high | A |
| 07 | Keep Dynamic actors awake while the viewport has no size | high | A |
| 08 | Dispose the remaining actors when the Kubriko instance is disposed | medium | A |
| 09 | Stop the current tick when the Kubriko instance is disposed during it | medium | A |
| 10 | Make TickSource start and stop safe across threads | high | A |
| 11 | Validate the fixed-frequency tick rate before computing its interval | low | A |
| 12 | Restart the viewport's effects when a different Kubriko instance is passed | medium | A |
| 13 | Report the viewport as unfocused when it leaves composition | medium | A |
| 14 | Apply a Manager's modifiers only once that Manager is initialized | medium | A |
| 15 | Re-anchor the viewport frame loop after a long gap between frames | medium | A |
| 16 | Make the synchronous StateFlows emit their current value to new collectors | medium | A |
| 17 | Make the Manager lookup cache safe across threads | low | A |
| 18 | Document that the first Manager of a class wins | low | A |
| 19 | Document the rules for sharing a Manager between instances; stop updating a disposed one | medium | A |
| 20 | Fix Timer's edge cases and keep repeating timers on schedule | medium | A |
| 21 | Fill untextured vertices with both default texture coordinates | low | A |
| 22 | Convert screen sizes to scene sizes without the position offset | medium | A |
| 23 | Convert degrees without wrapping; take AngleDegrees' sine and cosine in radians | medium | A |
| 24 | Ignore non-finite camera positions and scale factors | low | A |
| 25 | Correct stale engine documentation | low | A |
| 30 | Give each desktop music playback job its own decoder chain | high | B |
| 31 | Dispose every loaded player in `MusicManager.unloadAll()` | medium | B |
| 32 | Resume each Android sound load with its own sample id | high | B |
| 33 | Share one in-flight load per audio URI between preload and play | high | B |
| 34 | Settle failed audio loads instead of crashing or hanging | high | B |
| 35 | Limit Android sound streams per sound, as documented | medium | B |
| 36 | Reset web music state when a track ends | medium | B |
| 37 | Close the AudioContext used to decode web music | low | B |
| 38 | Make the desktop sound cache thread-safe | low | B |
| 39 | Release keys on focus loss on the main thread | medium | B |
| 40 | Run the Android key-release debounce on the main thread | medium | B |
| 41 | Forget held web keys when the window loses focus | medium | B |
| 42 | Release pointers on focus loss on the main thread | medium | B |
| 43 | Keep gamepads connected across focus loss | medium | B |
| 44 | Survive a blocked Gamepad API on the web | medium | B |
| 45 | Save iOS strings under the prefixed key | high | C |
| 46 | Fall back to defaults when a persisted value is missing or fails | high | C |
| 47 | Re-attach particle batches after the actor manager drops them | high | C |
| 48 | De-duplicate shaders only when they are true duplicates | medium | C |
| 49 | Clamp the physics tick delta before accumulating it | low | C |
| 50 | Start the physics body lists at initialization | low | C |
| 51 | Keep the physics sweep sorted when a body is NaN | medium | C |
| 52 | Keep applying explosion impulses after a body at the epicenter | medium | C |
| 53 | Fix the raycast hit point and distance for circle bodies | medium | C |
| 54 | Keep non-finite masks from collapsing the spatial hash grid | medium | C |
| 55 | Make empty and zero-size polygon masks behave like points | medium | C |
| 56 | Detect collisions for point masks | low | C |
| 57 | Retry failed sprite loads; drop loads unloaded mid-flight | medium | C |
| 58 | Wrap looping backward sprite animation into range | medium | C |
| 59 | Fix the frame row for sprite sheets rotated by 270° | low | C |
| 60 | Make `deserializeActors` fail as a whole for any bad actor | medium | C |
| 61 | Default a missing BoxBody pivot to the decoded size's center | low | C |
| 65 | Render the game viewport from the noop debug menu | high | D |
| 66 | Apply `OverlayOnly`'s modifier to its root | medium | D |
| 67 | Key debug overlays by Kubriko instance and release them on dispose | medium | D |
| 68 | Give the debug overlay its own ViewportManager | medium | D |
| 69 | Dispose the scene editor when it leaves composition | medium | D |
| 70 | Stop Escape from closing the scene editor over unsaved changes | high | D |
| 71 | Refuse unreadable scene files; report load and save failures | high | D |
| 72 | Snapshot undo state from an editor-owned actor list | medium | D |
| 73 | Match String properties by class; render null as empty | medium | D |
| 74 | Format and parse editor numbers locale-independently | medium | D |
| 75 | Bound the scene editor grid by an integer line count | medium | D |
| 76 | Pin the published scene editor to the real debug menu | low | D |
| 77 | Anchor the iOS share sheet and present it from the top controller | high | D |
| 78 | Track the current resource in the preloaded resource states | low | D |
| 79 | Apply caller modifiers to one root in SmallSlider and LoadingOverlay | low | D |
| 80 | Keep the desktop content and title bar listener across full screen | high | E |
| 81 | Restore the Windows window position after full screen | medium | E |
| 82 | Ignore links the platform cannot open | medium | E |
| 83 | Collect back navigation intents inside their effect | low | E |
| 84 | Hide unfinished games from deeplinks too | low | E |
| 85 | Derive the menu scroll index from the menu rows | low | E |
| 86 | Remove the unobserved iOS window size notifications | low | E |
| 87 | Hide the iOS status bar through SwiftUI in full screen | low | E |
| 88 | Reset web full screen state when the browser refuses | low | E |
| 90 | Count Wallbreaker bricks on the tick thread; add persistent actors once | medium | F |
| 91 | Make the games' sound-effect queues thread-safe | medium | F |
| 92 | Create the game Kubriko before reading its StateManager | medium | F |
| 93 | Require the state holder in every example composable | low | F |
| 94 | Stop the slingshot stretch sound when removed mid-aim | medium | F |
| 95 | Cancel the previous Annoyed Penguins level load | low | F |
| 96 | Keep the physics demo's self-removing objects awake | low | F |
| 97 | Size the isometric demo's culling region to the projected view | medium | F |
| 98 | Arm Space Squadron's Spacebar guard only on game over | low | F |
| 99 | Keep Annoyed Penguins' game viewport composed while a level loads (companion of `13`) | medium | F |

## Lanes

Every lane applies its plans in numeric order (dependencies are already encoded in the numbering: `32` → `33` → `34`;
`45` → `46`; `49`–`51` share `PhysicsManagerImpl.kt`; `67` → `68`; `69` → `72` share `EditorController.kt`; `84` →
`85`; `92` → `93` → `99`; `32` → `33` → `34`, then `38`; `55` → `56`).

| Lane | Area | Plans | Artifacts | Files owned |
|---|---|---|---|---|
| A | engine core | 01–25 | `engine` | `engine/**`, root `CLAUDE.md`, `documentation/TICK_SOURCE.md`, and Annoyed Penguins' `Penguin.kt`, `StarIndicator.kt`, `base/DestructiblePhysicsObject.kt` (for `22`), and the `onUpdate` hunk of Annoyed Penguins' `managers/GameplayManager.kt` (for `20`) |
| B | plugins: audio & input | 30–44 | `plugin-audio-playback`, `plugin-keyboard-input`, `plugin-pointer-input`, `plugin-gamepad-input` | `plugins/{audio-playback,keyboard-input,pointer-input,gamepad-input}/**` |
| C | plugins: data, physics & rendering | 45–61 | `plugin-persistence`, `-particles`, `-shaders`, `-physics`, `-collision`, `-sprites`, `-serialization` | `plugins/{persistence,particles,shaders,physics,collision,sprites,serialization}/**`, `documentation/GETTING_STARTED_08.md` (only if `56` takes option B) |
| D | tools | 65–79 | `tool-debug-menu*`, `tool-scene-editor*`, `tool-ui-components` | `tools/**` |
| E | Showcase app | 80–88 | — (not published) | `app/**` |
| F | examples | 90–99 | — (not published) | `examples/**` except lane A's three Annoyed Penguins files; shares `managers/GameplayManager.kt` with lane A (`95` edits `onInitialize`/`loadScene`, `20` edits `onUpdate`) |

**Merge order: A → B → C → D → F → E.** A first because every other lane runs on the engine and its
ActorManager fixes (`03`, `04`, `06`) change callback timing the plugin, tool and example fixes were checked against.
Plugins next, then tools (which consume plugins — `71` relies on `60`'s failure semantics but works either way),
then examples, and the Showcase app last because it composes all of them.

**Shared files.** One file is edited by two lanes: Annoyed Penguins' `GameplayManager.kt` (lane A's `20` edits `onUpdate`, lane F's `95` edits `onInitialize`/`loadScene`; separate hunks, so the cherry-pick should apply cleanly). Otherwise no two lanes edit the same file. Root `CLAUDE.md` is edited by `00` (before the lanes) and then only
by lane A; each plugin/tool/example keeps its own `CLAUDE.md`. `strings.xml`: lane D edits only
`tools/scene-editor/src/desktopMain/composeResources/values/strings.xml`; lane E adds an app string only if decision
`82` goes against the recommendation. If a cherry-pick still conflicts, merge `CLAUDE.md` paragraphs and `strings.xml`
word by word, keeping every sentence and key from both sides.

**Cross-lane notes** (all already reflected in the plans): `05` removes the trigger for a sprite that stays unloaded
forever when `SpriteManager.get()` runs before initialization — `57` does not cover that case itself. `47`, `90`,
`94` and `95` were checked against both the `0008d027` ActorManager and the one after `03`/`04`. `48` and `90` are
independent. `65` leaves the Showcase's `IS_DEBUG_MENU_ENABLED` workaround in `ShowcaseContent.kt` untouched.

## Decisions — answered 2026-09-28

The user took the recommended option for all 35 (05, 01, 22 and 23 asked individually; the rest accepted as a
batch). Execute every plan with its recommended option. Each plan's Fix section still spells out both.

| # | Question | Answer |
|---|---|---|
| 01 | A throwing `onAdded`/`dispose`/`onRemoved`: crash or only log? | Finish the batch, publish, then rethrow the first failure through the Kubriko scope |
| 03 | A child shared by two Groups: refcount or document? | Document (removing either Group removes it) |
| 04 | Callbacks for add-then-remove / remove-then-add in one batch | `onAdded` → `dispose` → `onRemoved` / none |
| 05 | Hold actor processing until `start()` has initialized every Manager? | Yes — `allActors` stays empty until then; operations queued before `start()` are applied synchronously inside it (amended after review, so `start(); tick()` is deterministic headless) |
| 06 | Also hold back removal callbacks until the tick has dropped the actor? | No — publish derived lists synchronously and refresh before the update loop |
| 07 | With no viewport size, treat every Dynamic actor as active? | Yes |
| 08 | What `kubriko.dispose()` calls on remaining actors | `Disposable.dispose()` only, not `onRemoved()` |
| 13 | Report "not focused" when the viewport leaves composition? | Yes |
| 14 | Stop calling `processModifier`/`processOverlayModifier` before a Manager is initialized? | Yes |
| 15 | Treat a frame gap above 2 s as a resume and restart the timeline? | Yes |
| 18 | First-vs-last Manager of a type: fix docs or code? | Docs |
| 19 | Shared Managers: refcount or document the rules? | Document, and skip `onUpdate` once disposed |
| 20 | Carry a repeating Timer's overshoot; fire zero-length one-shots? | Yes to both |
| 22 | Fix `Size.toSceneSize`'s formula and deprecate its `viewportSize` parameter? | Yes (Annoyed Penguins' call sites keep their numbers) |
| 23 | Make `AngleDegrees.rad` a plain conversion; fix `sin`/`cos` taking degrees as radians? | Yes |
| 24 | Non-finite camera/scale input: ignore or throw? | Ignore, keep the last valid value |
| 34 | Does a failed audio URI count as settled in loading progress, and is it retried? | Yes (option A) |
| 35 | Android: enforce the per-sound stream limit the KDoc promises? | Yes, per sound |
| 43 | Keep gamepads connected across a focus loss? | Yes (option A) |
| 45 | iOS: read the old bare key once as a fallback? | Yes, never delete it |
| 48 | Which shaders count as duplicates? | Same class, same `layerIndex`, equal state |
| 52 | Change the simulation so bodies after an epicenter body get their impulse? | Yes |
| 53 | Change where raycast explosions hit circles? | Yes |
| 56 | Point masks: implement collisions or document they never collide? | Implement |
| 60 | One bad actor: fail the whole scene or skip it? | Fail the whole scene (logged) |
| 61 | Missing `pivot` in JSON → center of the decoded size? | Yes |
| 65 | Noop debug menu renders the wrapped viewport (as its README says)? | Yes |
| 66 | `OverlayOnly`'s modifier goes on its root? | Yes |
| 68 | Overlay assumes a centered game canvas, or new engine getter for the aspect mode? | Assume centered (no engine API) |
| 69 | Scene editor disposes its instances (and the caller's managers) on leaving composition? | Yes |
| 70 | Escape closes the editor only when unmodified and no text field is focused, or never? | Only when unmodified |
| 76 | Published scene editor: always real debug menu, or refuse to publish unless the flag is on? | Refuse to publish unless the flag is on |
| 79 | Caller's modifier on the root only in SmallSlider/LoadingOverlay? | Yes |
| 82 | A link that can't be opened: fail silently or tell the user? | Silently |
| 93 | Example composables: make `stateHolder` required, or remember a default? | Required |

## Checked and found solid

- **Engine:** the viewport tick loop's throttle maths (`Limit`/`DisplayDivider` sleep sizing, catch-up guard,
  re-anchoring on gated resume); `TargetFrameRate` validation; ActorManager's caller-thread ordering, Unique
  replacement, draw-cache reuse and `-0f` comparator; `BoxBody` AABB vs. draw transform and pivot clamping;
  `TriangleBatch` capacity, 16-bit indices and Skia bucket padding; the web bridge's export/arity fallback and heap
  refresh; the Android texture probe and refresh-rate hint; TickSource idempotence and `ManualTickSource`;
  `KubrikoImpl.dispose` idempotence; `AngleRadians` helpers; `Limit(60)` really is the intended frame-rate default.
- **Stress run:** no heap growth over 10k ticks at 10k actors; ~100 B allocated per tick regardless of actor count;
  dispose during in-flight batches, from another thread and twice is clean; 300 instances × 8 threads of `get()`
  raised nothing; the drawing-order comparator survives NaN/±0/±∞.
- **Plugins:** collision narrow phase, raycast and `SpatialHashGrid` bookkeeping; physics accumulator guard, arbiter
  pool, static handling, positive polygon mass; gamepad reference counting and dead zones; shader uniforms and the
  Android pre-33 fallback; sprite `SpriteResource` race handling; pointer cancellation grace; desktop `CachedSound`;
  Android/desktop/web persistence key prefixing; `serializeActors` always writes the pivot.
- **Tools:** Logger thread safety and bound; -api/real/noop signatures match exactly; `TextInput` focus balance;
  file-dialog cancel handling; `UndoRedoHistory` stack logic; camera animation cancellation; unknown persisted enum
  fallbacks.
- **App:** StateHolder create/dispose in `ExampleScreen` across Android recreation, Windows window recreation and
  quick re-selection; build-flag wiring for tests/debug menu/scene editor; Android full screen, splash and predictive
  back; desktop `WindowStateListener` pairing; web listener cleanup; Xcode schemes and `.run` configurations.
- **Examples:** every state holder disposes every Kubriko it owns (including the isometric demo's two and the shader
  animations' five); shared Managers across one example's instances; input release and pausing on focus loss;
  high-score persistence; `test-*` vs. `-noop` API parity.

## Dropped after verification

- **Physics penetration correction weights the heavier body** — inherited from JPhysics; the divide-by-zero needs two
  non-static zero-mass bodies, which the broad phase already skips; fixing it would change every simulation.
- **ParticleManager shared between instances** — a general `manager<T>()` limitation, covered by `19`'s documentation.
- **test-audio loading-progress flicker** — `collectAsState` keeps its last value across a new flow, so nothing flickers.
- **Scene-editor wrappers never dispose their state holders** — those holders' Kubriko instances are lazy and never
  created; disposing them would create and then dispose shared Managers.
- **Physics demo chain lags into sleep** — disproven (links lag 1–2 frames, sleep needs ~960 units); re-scoped into `96`.
- **Web `initialPath` never strips its prefix** — history navigation depends on exactly that; dead code, not a bug.
- **Web `rootPath` from the first path** — only wrong when the dev server is opened on a deep path.
- **Web `history.back()` race** and **Timer `Long` overflow** — not reachable in practice.

## Not filed (real, but outside this sweep's budget or angle)

- The debug menu's metadata labels ("FPS:", "Actors:" in `DebugMenuContents.kt`) are inline literals, against the
  `strings.xml` rule — worth one small follow-up plan.
- `Logger.log` copies the whole list per entry (correct, bounded, only with logging on).
- `[Desktop] Showcase Release` signs with a hard-coded identity, so it fails on other contributors' Macs.
- With `fixedRate`/`fixedFrequency`, `onUpdate` runs on `Dispatchers.Default` and races main-thread input events in
  every input plugin — a design limitation of those tick sources.
- A focus-loss synthesized pointer release launches Annoyed Penguins' penguin mid-aim (documented plugin behaviour).
- `app/ios/CLAUDE.md`'s Publishing bullet still says the only Xcode scheme lives under `xcuserdata` (shared since `d3a55c03`).
- Ctrl/Cmd+Z in a focused scene-editor text field also undoes the scene; `LogEntry` timestamps are not zero-padded.

## Measurements (live stress run, desktop JVM 21, Apple Silicon)

| Scenario | Size | Result |
|---|---|---|
| Add in one batch → published | 1k / 10k / 50k | 3 / 6 / 21 ms |
| Tick, all actors moving | 1k / 10k / 50k | 7–23 / 41 / 205 µs per tick; ~100 B/tick flat |
| Heap over 10k ticks | 10k actors | 9.3 MB → 9.3 MB |
| Churn 5000 ticks × 20 adds + 20 removes | pool 2000 | 506 actors duplicated (up to 10×), 1432 callback mismatches |
| `start()`/`stop()` racing on two threads | — | 7 leaked tick loops, 1605 ticks/s instead of 200 |
| Group 2-cycle | 2 | processor hangs, heap 20 MB → 1.6 GB in 2.5 s |
| Collision, 3000 boxes | + 1 NaN / ∞ mask | 616 µs → 65 ms per tick |
| Physics, 2000 balls | + 1 NaN body | 40–50% of contacts lost |

Probe sources are kept in the session scratchpad (`stress/`, `laneC/`), not in the repository.

## Manual checks owed

Each plan's **Manual check** section is authoritative; these are the ones that need a device, OS or browser:

- **iOS device:** `45` (persist a string, relaunch), `77` (Share on an iPad), `87` (status bar in full screen), `86`.
- **Android device:** `32`, `35`, `40` (hardware keyboard), `34` (missing asset), `82` (device without a mail app),
  `36`/`41`/`44` equivalents on the web.
- **Web browser:** `36`, `37`, `41` (alt-tab while holding a key), `44` (iframe without `allow="gamepad"`), `88`
  (refused full screen), `57` (throttled network).
- **macOS desktop:** `80` (full screen round trip, then drag and double-click the title strip). **Windows:** `81`.
- **Desktop, scene editor:** `69`–`75` (Escape with unsaved changes, opening a non-JSON file, a comma-decimal locale,
  minimum zoom with snap 1), `65`/`66` (Showcase with `showcase.isDebugMenuEnabled=false`, then the editor's debug panel
  in a landscape window).
- **Showcase games:** `90` (Wallbreaker level clear on Android/desktop), `94` (aim while the last star is collected),
  `95`, `99`/`13` (pick every Annoyed Penguins level: it must start running, not paused), `97` (isometric corners at default zoom in a 1920×1080 window), `43` (gamepad across alt-tab).
