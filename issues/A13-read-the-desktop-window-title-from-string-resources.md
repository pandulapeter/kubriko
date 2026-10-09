# Read the desktop window title from a string resource instead of a literal

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/desktop/src/main/composeResources/values/strings.xml` (new)
- `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt`

## Problem
`KubrikoShowcaseApp.kt:76` (at 2480325f) passes a literal to the window:

```kotlin
SwingWindow(
    onCloseRequest = ::exitApplication,
    state = windowState,
    title = "Kubriko Showcase",
```

The title is user-facing (Linux title bar, the taskbar / window switcher on every OS), and `code-style` puts every
on-screen string, window titles included, in `composeResources/values/strings.xml`.

`app/desktop` already has Compose resources: `src/main/composeResources/drawable/ic_icon.webp`, read through
`kubriko.app.desktop.generated.resources.Res` (imported at :39-40), and `implementation(libs.compose.resources)` in its
build file, so adding `values/strings.xml` needs no build change. (`app/shared`'s `kubriko_showcase` string cannot be
reused: its generated `Res` is internal to `app/shared`.)

## Fix
1. Create `app/desktop/src/main/composeResources/values/strings.xml` with the XML MPL-2.0 header copied from
   `app/shared/src/commonMain/composeResources/values/strings.xml` and one entry:
   `<string name="kubriko_showcase">Kubriko Showcase</string>`.
2. In `KubrikoShowcaseApp.kt`: `title = stringResource(Res.string.kubriko_showcase),` with imports
   `kubriko.app.desktop.generated.resources.kubriko_showcase` and `org.jetbrains.compose.resources.stringResource`.
   `KubrikoShowcaseWindow` is already a `@Composable`, so the call is legal there.

`System.setProperty("apple.awt.application.name", "Kubriko Showcase")` (:52) stays a literal: it runs before Compose
starts, outside any composition (exempt as a platform identifier set before the UI exists).

## Behaviour
On the JVM, Compose resources resolve `stringResource` synchronously on first composition, so the window gets the same
title before it is shown. Unchanged.

## Public API
None.

## Tests
None (a resource lookup).

## Verify
`./gradlew :app:desktop:compileKotlin` (the `generateComposeResClass` step must produce `Res.string.kubriko_showcase`).

## Manual check
`./gradlew :app:desktop:run` on Linux (or any OS's window switcher): the title still reads "Kubriko Showcase".
