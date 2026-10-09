# Move the state holder pool out of `ExampleScreen.kt` into `ShowcaseStateHolders.kt`, listing each factory call once

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ExampleScreen.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseStateHolders.kt` (new)
- `app/shared/CLAUDE.md`

## Problem
`ExampleScreen.kt` (346 lines at 2480325f) spells out every one of the 15 state holder factory calls twice, with
their arguments — once inside the Composable and once in `getStateHolder()`:

```kotlin
// ShowcaseEntry.ExampleScreen (:76-86)
ShowcaseEntry.WALLBREAKER -> WallbreakerGame(
    stateHolder = getOrCreateState(stateHolders) {
        createWallbreakerGameStateHolder(
            webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
            isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
        )
    },
// ShowcaseEntry.getStateHolder() (:232-238)
ShowcaseEntry.WALLBREAKER -> getOrCreateState(stateHolders) {
    createWallbreakerGameStateHolder(
        webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
        isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
    )
}
```

Adding an example, or a new factory argument, has to be done in both tables, and a mismatch silently creates the holder
with different arguments depending on which path created it first. The pool itself (`private val stateHolders` :66,
`getOrCreateState` :325-328, `stateHolderType` :330-347) and the disposal in `onDispose` (:221-228) sit in the screen
file too.

## Fix
A mechanical move plus one accessor per entry; the process-wide pool, its key (the holder's type) and every point
where a holder is created on read stay exactly as they are.

New `.../implementation/ui/ShowcaseStateHolders.kt` (same package, MPL-2.0 header from `ExampleScreen.kt`) receives,
verbatim:
- `private val stateHolders = mutableStateOf(emptyList<StateHolder>())`
- `private inline fun <reified T : StateHolder> getOrCreateState(stateHolders: MutableState<List<StateHolder>>, creator: () -> T): T`
- `private val ShowcaseEntry.stateHolderType`

and gains:
- One `internal fun` per entry that holds that entry's single `getOrCreateState(stateHolders) { create…(…) }` call,
  copied from the `getStateHolder()` branch (the two copies are identical at 2480325f — confirm with a diff of the two
  tables before deleting one), returning the concrete type the screen needs, e.g.

  ```kotlin
  internal fun wallbreakerGameStateHolder() = getOrCreateState(stateHolders) {
      createWallbreakerGameStateHolder(
          webRootPathName = BuildConfig.WEB_ROOT_PATH_NAME,
          isLoggingEnabled = BuildConfig.IS_DEBUG_MENU_ENABLED,
      )
  }
  internal fun licensesScreenStateHolder() = getOrCreateState(stateHolders, ::createLicensesScreenStateHolder)
  ```

  Names: `<camelCase of the factory without "create">` — `wallbreakerGameStateHolder`, `spaceSquadronGameStateHolder`,
  `annoyedPenguinsGameStateHolder`, `blockysJourneyGameStateHolder`, `contentShadersDemoStateHolder`,
  `isometricGraphicsDemoStateHolder`, `particlesDemoStateHolder`, `performanceDemoStateHolder`,
  `physicsDemoStateHolder`, `shaderAnimationsDemoStateHolder`, `audioTestStateHolder`, `collisionTestStateHolder`,
  `inputTestStateHolder`, `licensesScreenStateHolder`, `aboutScreenStateHolder`. Check the package for clashes
  (none at 2480325f).
- `internal fun ShowcaseEntry.getStateHolder()` moved here, each branch now calling its accessor
  (`ShowcaseEntry.WALLBREAKER -> wallbreakerGameStateHolder()`). Keep its return type inferred, as it is today.
- `internal fun ShowcaseEntry.disposeStateHolder()` holding the body of today's `onDispose` branch, verbatim:

  ```kotlin
  stateHolderType.let { type ->
      stateHolders.value.filter { type.isInstance(it) }.forEach { it.dispose() }
      stateHolders.value = stateHolders.value.filterNot { type.isInstance(it) }
  }
  ```

`ExampleScreen.kt` keeps `ShowcaseEntry.ExampleScreen`; each branch passes `stateHolder = <accessor>()` instead of the
inline `getOrCreateState { … }`, and the effect becomes

```kotlin
DisposableEffect(type) {
    onDispose {
        if (getSelectedShowcaseEntry() != this@ExampleScreen) {
            disposeStateHolder()
        }
    }
}
```

Move the factory / `*StateHolder` type imports to the new file; `ExampleScreen.kt` keeps the screen Composable imports
(`WallbreakerGame`, …) and drops `MutableState`, `mutableStateOf`, `StateHolder`, `BuildConfig` and the factories.

`app/shared/CLAUDE.md`: in "StateHolder lifecycle and multi-instance management", "`stateHolders` in `ExampleScreen.kt`"
→ "`stateHolders` in `ShowcaseStateHolders.kt`"; Key files: change the `ExampleScreen.kt` line to "per-entry
`ExampleScreen` Composable and its disposal effect" and add "`implementation/ui/ShowcaseStateHolders.kt` — the `StateHolder`
pool: one accessor per entry, `getStateHolder()`, `disposeStateHolder()`." Grep the repo for `ExampleScreen.kt` /
`getOrCreateState` in docs and skills (ignore `.claude/settings.local.json`, a local permission list) and fix stale ones.

## Behaviour
Each holder is still created lazily the first time either path reads it (composition of `ExampleScreen`, the back
handler, `BoxWithConstraints` or the selection callback in `KubrikoShowcase.kt`), with the same arguments, into the same
process-wide `mutableStateOf` list, keyed by the same reified type; disposal runs the same code under the same condition
at the same time. The only difference is one extra non-inline function frame per lookup. The structural problems of the
pool (creation on read inside composition, the global itself) are the Planned A51 and are deliberately not touched here.

## Public API
None.

## Tests
The existing ones (the pool reads `BuildConfig` and creates real Kubriko instances; a unit test would need the example
modules' Skia-free construction paths and is not part of a mechanical move).

## Verify
`./gradlew :app:shared:compileKotlinDesktop :app:shared:desktopTest :app:desktop:compileKotlin`

## Manual check
Open two examples in turn on desktop (`./gradlew :app:desktop:run`), go back to the menu, re-open the first: it starts
fresh (the old holder was disposed) and its music stops when leaving.
