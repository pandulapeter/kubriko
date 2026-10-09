# Replace the file-level selection and state holder globals with one process-scoped `ShowcaseSession` passed down from the root

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcase.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseSession.kt` (new)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseStateHolders.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ExampleScreen.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseContent.kt`
- `app/shared/src/commonTest/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseSessionTest.kt` (new)
- `app/shared/CLAUDE.md`

**Rebased:** on 70de96c6 after the Now plans landed.

A08 (61847b82, the pool in `ShowcaseStateHolders.kt` with one accessor per entry) and A10 (1f8496b9, `ShowcaseContent`'s
named private Composables) have landed; the quotes below are at 70de96c6.

## Problem
Two process-wide mutable globals are read service-locator style from inside composition (at 70de96c6):

```kotlin
private val selectedShowcaseEntry = mutableStateOf<ShowcaseEntry?>(null)   // KubrikoShowcase.kt:125
private val stateHolders = mutableStateOf(emptyList<StateHolder>())         // ShowcaseStateHolders.kt:48
```

- `selectedShowcaseEntry.value?.getStateHolder()` (`KubrikoShowcase.kt:79, 93`) **creates** a holder on read, writing
  `stateHolders` during the composition that read it (a write-after-read of the same snapshot state in one composition).
- The selection callback builds the new entry's holder just to compare it (:109):
  `if (showcaseEntry?.getStateHolder() != activeStateHolder)` — equivalent to comparing the entries, since each entry maps
  to its own holder type, but with the side effect of creating the holder at click time.
- `ShowcaseContent` takes both `selectedShowcaseEntry` and `getSelectedShowcaseEntry = { selectedShowcaseEntry.value }`
  (`KubrikoShowcase.kt:106-107`) only so `ExampleScreen`'s `onDispose` (`ExampleScreen.kt:123-129`,
  `if (getSelectedShowcaseEntry() != this@ExampleScreen) disposeStateHolder()`) can read the global's latest value; the
  lambda is threaded through `ShowcaseContent` (:81) → `ContentWithSideMenu` (:183) → `ShowcaseEntryContent` (:308).
- None of this can be unit-tested (`code-style`: no service-locator lookups in Composables; a long-lived object takes what
  it needs).

The process scope is deliberate and must stay: the Android Activity is recreated on configuration changes and a running
game must survive it.

## Fix
`internal class ShowcaseSession` (one instance per process, created where the globals are today), owning:
- `selectedEntry: State<ShowcaseEntry?>` and `select(entry: ShowcaseEntry?)`;
- `holderFor(entry): StateHolder` (get-or-create, today's per-entry accessors and `getStateHolder()` in
  `ShowcaseStateHolders.kt`) and `release(entry)` (today's `disposeStateHolder()`, :159-164);
- `isSelected(entry)` for `ExampleScreen`'s `onDispose`, replacing `getSelectedShowcaseEntry`.

`KubrikoShowcase` passes the session (or the slices each child uses) down; `ShowcaseContent`, `ContentWithSideMenu`,
`ShowcaseEntryContent` and `ExampleScreen` drop `getSelectedShowcaseEntry`; the selection callback compares entries (`showcaseEntry != session.selectedEntry.value`) and
calls `stopMusic()` on the active holder only if one exists.

## Decision
1. Where the holder for the selected entry is first created: (a) keep creating on read in composition, as today (zero
   behaviour change, keeps the write-in-composition); (b) create in `select()` — outside composition, before the
   frame that shows the entry. **Recommended: (b)**, with `ExampleScreen` only reading. It changes *when* a Kubriko
   instance is built (on click instead of during the next composition), which is observable only in timing.
2. How the session reaches the tree: (a) parameters; (b) a `CompositionLocal` provided in `KubrikoShowcase`.
   **Recommended: (a)** — the tree is shallow and parameters keep each child's inputs explicit.
3. `KubrikoShowcase`'s defaults (`KubrikoShowcase.kt:49-50`) `deeplink = selectedShowcaseEntry.value.deeplink` and
   `onDestinationChanged = { selectedShowcaseEntry.value = it.processDeeplink() }` read
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
