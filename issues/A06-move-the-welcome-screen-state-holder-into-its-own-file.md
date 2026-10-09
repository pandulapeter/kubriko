# Move `WelcomeScreenStateHolder` out of `WelcomeScreen.kt` into `WelcomeScreenStateHolder.kt`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/welcome/WelcomeScreen.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/welcome/WelcomeScreenStateHolder.kt` (new)

## Problem
`WelcomeScreen.kt` (at 2480325f, :208-243) also declares

```kotlin
internal sealed interface WelcomeScreenStateHolder : StateHolder {

    companion object {
        val shouldShowMoreInfo = mutableStateOf(false)

        @Composable
        fun areResourcesLoaded() = areIconResourcesLoaded() && areStringResourcesLoaded()
```

read by `WelcomeScreen` (:89, :162, :170) and by `ResourceLoader.areResourcesLoaded()`. `code-style`: one file, one thing.
(The interface has no implementation and is used as a namespace; reshaping that is the Planned A52 — this plan only moves
it.)

## Fix
Move `WelcomeScreenStateHolder` verbatim to `.../implementation/ui/welcome/WelcomeScreenStateHolder.kt` (same package,
MPL-2.0 header from `WelcomeScreen.kt`) with the imports it needs (`Composable`, `mutableStateOf`, `StateHolder`,
`preloadedImageVector`, `preloadedString`, `Res`, the `ic_github` / `ic_discord` / `ic_documentation` /
`ic_getting_started` drawables and the `welcome_*` strings its `areStringResourcesLoaded` reads). Remove from
`WelcomeScreen.kt` the imports it no longer uses (`StateHolder`, `preloadedImageVector`, `preloadedString`,
`mutableStateOf` if unused, and any `welcome_*` / `ic_*` the screen does not render). `ResourceLoader.kt`'s import
(`…ui.welcome.WelcomeScreenStateHolder`) is unchanged, since the package is.

## Behaviour
Verbatim move. Unchanged.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :app:shared:compileKotlinDesktop`

## Manual check
None.
