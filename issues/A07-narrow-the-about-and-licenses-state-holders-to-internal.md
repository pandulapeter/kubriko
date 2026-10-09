# Narrow the About and Licenses state holder factories and interfaces from public to internal

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/about/AboutScreenStateHolder.kt` (from A04)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/LicensesScreenStateHolder.kt` (from A05)

## Problem
Everything else under `com.pandulapeter.kubrikoShowcase.implementation` is `internal`, and the only public declaration
`app/shared` needs is `KubrikoShowcase`. Yet these four are public (at 2480325f, in `AboutScreen.kt:70,170` and
`LicensesScreen.kt:58,264`):

```kotlin
fun createAboutScreenStateHolder(): AboutScreenStateHolder = AboutScreenStateHolderImpl()
sealed interface AboutScreenStateHolder : StateHolder {
fun createLicensesScreenStateHolder(): LicensesScreenStateHolder = LicensesScreenStateHolderImpl()
sealed interface LicensesScreenStateHolder : StateHolder {
```

Their only callers are in `app/shared` (`ExampleScreen.kt`, `ShowcaseEntry.kt`, the screens themselves). Grepping the
whole repo and `../Tesselar` finds no other user; `app/android`, `app/desktop`, `app/web` and `app/ios` call only
`KubrikoShowcase` / `KubrikoShowcaseViewController`, and `app/ios` does not `export` `app/shared` into its framework, so
no Swift code sees them either.

## Fix
Add `internal` to the two factories and the two `sealed interface`s. Nothing else changes. A08, which lands next, moves
their references into `ShowcaseStateHolders.kt` in the same module, which `internal` allows.

## Behaviour
Visibility only, within an application module. Unchanged.

## Public API
None (`app/shared` is not published).

## Tests
The existing ones.

## Verify
`./gradlew :app:shared:compileKotlinDesktop :app:android:compileDebugKotlin :app:web:compileKotlinWasmJs :app:desktop:compileKotlin`

## Manual check
None.
