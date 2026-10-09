# Third review sweep — structure, SOLID, readability, testability

Reviewed at `2480325f` on `main` (2026-10-09). Angle: architecture and internal code quality — SOLID, readability,
developer satisfaction, testability, Compose best practices. Ground rules from the user: no public API change unless
absolutely necessary; behaviour changes only where they fix a bug.

**Status (2026-10-09):** 227 plans. The 179 Now plans landed as `92b16c51..70de96c6`; 44 of the 48 Planned plans
landed as `ac016029..b31bb3f7` (50 commits: E50, G59 and T53 asked for more than one). Unit tests, the full
`./gradlew build` and the bridge check are green. Four plans remain, each waiting on something outside the code.

## Remaining plans

| Plan | Fix | Waiting on |
|---|---|---|
| [A53](A53-follow-the-live-theme-for-the-showcase-surface-elevations.md) | Follow the live theme for the Showcase's surface elevations, and define them once | The plan's first step: on macOS, switch the system theme while the Showcase runs and see whether the elevations follow. If they do, the plan ends with no code change (outcome c). |
| [D53](D53-check-and-drop-test-audios-extra-viewport-insets-padding.md) | Drop test-audio's extra `windowInsetsPadding` on the `KubrikoViewport` if it changes nothing | A device check with insets (Android landscape with a cutout). Decision: remove only if the check shows no change. |
| [D56](D56-split-the-isometric-renderer-files-holding-several-types.md) | Split the isometric renderer files that hold several top-level types | Decision: leave until the next Tesselar sync, and do it in Tesselar first. |
| [D57](D57-share-one-scatter-loop-in-the-isometric-logic-manager.md) | Share one scatter loop for the isometric demo's NPCs, trees and bushes | Decision: leave until the next Tesselar sync, and do it in Tesselar first. |

Each is in lane A or D; none shares a file with another, so they can run in any order from `HEAD`.

## Decisions

Answered 2026-10-09: the recommended option for every plan. Do not re-ask. For the four left: **A53** app-only fix
reading darkness from the surface colour, after the manual macOS live-theme check; **D53** remove the extra padding only
if a device check shows no change; **D56/D57** leave the Tesselar-synced renderer and `LogicManager` until the next sync.

## Found while landing, for later plans

- **Game back navigation drift (G51)** — pinned as-is by each game's `BackNavigationTest`; each is a possible bug plan:
  Wallbreaker resumes a started game while the close confirmation is open; Blocky's Journey never resumes a paused game
  on back; Space Squadron's game-over "running" state falls through to the info/resume/fullscreen/close branches;
  Annoyed Penguins decides "resume" by whether a level is loaded, the others by `isGameStarted`; back sounds differ
  (Annoyed Penguins and Blocky's Journey play the toggle sound on every back; Wallbreaker's fullscreen exit uses the
  click sound).
- **Gamepad triggers (P55)** press only strictly above `triggerThreshold`, pinned by a test; the KDoc does not say.
- **Showcase session (A51)** — a holder for an example selected and replaced before it was ever composed stays in the
  session's pool until that example is selected again.
- **Test audio (D52)** — `MusicManager.play()` ignores a track that is already playing, so the new loop toggle is
  disabled while its track plays.

## For the release notes

- `plugin-physics`: `RaycastExplosion` / `RayScatter.castRays` now cast `noOfRays` rays and push the nearest body each
  hits; calling `castRays` again replaces the rays (P50). Before, the explosion did nothing.
- `plugin-pointer-input` (desktop): `tryToMoveHoveringPointer` returns `false` while the engine's `windowState` is unset
  instead of throwing (E51).
- `engine`: `InternalViewport` is deprecated, to become internal in the next binary-breaking release (E54).
- `tool-scene-editor`: `Float.toDifference()` is deprecated, to become internal in the next binary-breaking release (T54).
- `tool-logger`'s POM no longer depends on `kotlinx-datetime` (T25, from the Now plans).

## Manual checks owed

From the Planned plans: the Showcase on desktop, web and the iOS simulator after the engine splits (E50, E52, E55 —
Performance, Shader, Isometric and sprite demos; frame-rate targets on 60/120 Hz; the web bridge fast path); cursor
snapping in Space Squadron and Wallbreaker (E51); a physics demo explosion (P50); gamepad focus navigation on desktop and
Android (P56); a full scene editor pass — selection, undo/redo, placement preview, drags, camera, connected mode for
the performance and physics demos (T50–T53, G55, D50); debug menu timestamps (T56); the desktop fullscreen round trip on
Windows, macOS and Linux (A50); Android rotation, fast example switching, compact back and the web deeplink (A51, A52);
the Licenses screen (A54); a before/after layout comparison of each game (G54), hover, fonts, back on Android, web cold
start, Annoyed Penguins' map names, Space Squadron's score, Wallbreaker's corner bounce (G50–G59); the info panel
toggle carrying over between examples and across rotation (G60); the demos' controls panels, the shader fallback on
Android 12, the loading overlay fade and the audio loop toggle (D51–D60); the isometric joystick on touch (D55).

From the Now plans: the scene editor (T08–T18, D15), the debug menu (T19–T22), the Showcase on desktop (A02, A10, A13,
A14), web (A15) and Android, every game and demo screen after its overlay extraction (G14–G27, D02–D47), the isometric
joystick in the wide windowed layout (D46).
