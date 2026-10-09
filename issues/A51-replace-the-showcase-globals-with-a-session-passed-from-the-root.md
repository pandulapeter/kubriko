# Replace the file-level selection and state holder globals with one process-scoped `ShowcaseSession` passed down from the root

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcase.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseSession.kt` (new)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseStateHolders.kt` (from A08)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ExampleScreen.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseContent.kt`
- `app/shared/src/commonTest/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseSessionTest.kt` (new)
- `app/shared/CLAUDE.md`

Builds on A08 (pool in `ShowcaseStateHolders.kt`) and A10 (`ShowcaseContent` structure); re-check quotes at the new HEAD.

## Problem
Two process-wide mutable globals are read service-locator style from inside composition (at 2480325f):

```kotlin
private val selectedShowcaseEntry = mutableStateOf<ShowcaseEntry?>(null)   // KubrikoShowcase.kt:148
private val stateHolders = mutableStateOf(emptyList<StateHolder>())         // ExampleScreen.kt:66
```

- `selectedShowcaseEntry.value?.getStateHolder()` (`KubrikoShowcase.kt:78, 92`) **creates** a holder on read, writing
  `stateHolders` during the composition that read it (a write-after-read of the same snapshot state in one composition).
- The selection callback builds the new entry's holder just to compare it (:108):
  `if (showcaseEntry?.getStateHolder() != activeStateHolder)` — equivalent to comparing the entries, since each entry maps
  to its own holder type, but with the side effect of creating the holder at click time.
- `ShowcaseContent` takes both `selectedShowcaseEntry` and `getSelectedShowcaseEntry = { selectedShowcaseEntry.value }`
  only so `ExampleScreen`'s `onDispose` can read the global's latest value.
- None of this can be unit-tested (`code-style`: no service-locator lookups in Composables; a long-lived object takes what
  it needs).

The process scope is deliberate and must stay: the Android Activity is recreated on configuration changes and a running
game must survive it.

## Fix
`internal class ShowcaseSession` (one instance per process, created where the globals are today), owning:
- `selectedEntry: State<ShowcaseEntry?>` and `select(entry: ShowcaseEntry?)`;
- `holderFor(entry): StateHolder` (get-or-create, today's accessors from A08) and `release(entry)` (today's
  `disposeStateHolder`);
- `isSelected(entry)` for `ExampleScreen`'s `onDispose`, replacing `getSelectedShowcaseEntry`.

`KubrikoShowcase` passes the session (or the slices each child uses) down; `ShowcaseContent` drops
`getSelectedShowcaseEntry`; the selection callback compares entries (`showcaseEntry != session.selectedEntry.value`) and
calls `stopMusic()` on the active holder only if one exists.

## Decision
1. Where the holder for the selected entry is first created: (a) keep creating on read in composition, as today (zero
   behaviour change, keeps the write-in-composition); (b) create in `select()` — outside composition, before the
   frame that shows the entry. **Recommended: (b)**, with `ExampleScreen` only reading. It changes *when* a Kubriko
   instance is built (on click instead of during the next composition), which is observable only in timing.
2. How the session reaches the tree: (a) parameters; (b) a `CompositionLocal` provided in `KubrikoShowcase`.
   **Recommended: (a)** — the tree is shallow and parameters keep each child's inputs explicit.
3. `KubrikoShowcase`'s defaults `deeplink = selectedShowcaseEntry.value.deeplink` and `onDestinationChanged = { … }` read
   the global; they must keep their behaviour (Android and iOS rely on the defaults) by reading the session instance.

## Behaviour
With 1(b): the clicked entry's holder is created at selection instead of in the following composition; disposal still
happens in `onDispose` when the departing entry is no longer selected, so the crossfade timing and `stopMusic()` order are
unchanged. Back navigation, deeplinks and process-scope survival unchanged.

## Public API
None (`KubrikoShowcase`'s signature and defaults keep their meaning).

## Tests
`ShowcaseSessionTest` with fake `StateHolder`s (`kubriko = emptyFlow()`, counting `dispose()`): `holderFor` returns the
same instance until `release`; `release` disposes exactly that entry's holder; `select` updates `selectedEntry`. The
session must therefore take its factory table as a constructor parameter (default: the real one) so tests can inject
fakes without creating Kubriko instances.

## Verify
`./gradlew :app:shared:desktopTest :app:desktop:compileKotlin :app:android:compileDebugKotlin :app:web:compileKotlinWasmJs`

## Manual check
Android: start a game, rotate the device — the game keeps running. All platforms: switch between examples quickly
(music stops on leaving, the previous example is disposed after the crossfade); back from an example in compact layout;
web deeplink load and browser back.
