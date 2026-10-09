# Move the About screen's state holder out of `AboutScreen.kt` into `AboutScreenStateHolder.kt`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/about/AboutScreen.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/about/AboutScreenStateHolder.kt` (new)

## Problem
`AboutScreen.kt` (211 lines at 2480325f) mixes the screen with its state holder:

- `fun createAboutScreenStateHolder(): AboutScreenStateHolder = AboutScreenStateHolderImpl()` (:70)
- `sealed interface AboutScreenStateHolder : StateHolder { companion object { @Composable fun areResourcesLoaded() … } }` (:170-203)
- `private class AboutScreenStateHolderImpl : AboutScreenStateHolder { … val platform = MetadataManager.newInstance().platform … }` (:205-212)

`code-style`: one file, one thing, named after it.

## Fix
Move those three declarations verbatim (order: factory, interface, impl) into
`.../implementation/ui/about/AboutScreenStateHolder.kt`, same package, MPL-2.0 header from `AboutScreen.kt`.

`AboutScreen` keeps `stateHolder as AboutScreenStateHolderImpl` (:79), so `AboutScreenStateHolderImpl` goes from `private`
to `internal` (no other declaration of that name in the package). Visibility of the factory and the interface stays as it
is — narrowing them is plan A07.

`AboutScreen.kt` keeps `AboutScreen` and `private MetadataManager.Platform.description()`. Move the imports only the state
holder needs (`Kubriko`, `StateHolder`, `preloadedImageVector`, `preloadedString`, `BuildConfig`, `Flow`, `emptyFlow`, and
the `Res` drawables / strings only `areResourcesLoaded` reads) and keep those both files use in both (`Res`,
`MetadataManager`, `Composable`, the `other_about_*` strings the screen also renders). Let the compiler's unused-import
warnings settle the exact split.

Grep the repo for `AboutScreen.kt` in docs and skills (none at 2480325f) in the same commit.

## Behaviour
Verbatim move in one package; only a `private` → `internal` widening. Unchanged.

## Public API
None (`app/shared` is not published).

## Tests
The existing ones.

## Verify
`./gradlew :app:shared:compileKotlinDesktop`

## Manual check
None.
