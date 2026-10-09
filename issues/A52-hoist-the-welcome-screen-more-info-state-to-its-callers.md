# Hoist the Welcome screen's "more info" state to its callers and dissolve the namespace-only `WelcomeScreenStateHolder`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/welcome/WelcomeScreen.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/welcome/WelcomeScreenStateHolder.kt` (renamed by the fix)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseContent.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ResourceLoader.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ShowcaseSession.kt` (if A51 landed)
- `app/shared/CLAUDE.md`

**Rebased:** on 70de96c6 after the Now plans landed.

A06 (b7602893, the holder's move into `WelcomeScreenStateHolder.kt`) and A10 (1f8496b9, `HomeContent`) have landed;
best after A51 (Planned), which gives the state a home.

## Problem
`WelcomeScreen` reads and writes a process-wide global through a type that is not a state holder (`WelcomeScreenStateHolder.kt:42-48`
at 70de96c6):

```kotlin
internal sealed interface WelcomeScreenStateHolder : StateHolder {   // no implementation exists
    companion object {
        val shouldShowMoreInfo = mutableStateOf(false)
```

used at `WelcomeScreen.kt:80` (`visible = !shouldUseCompactUi || WelcomeScreenStateHolder.shouldShowMoreInfo.value`),
`:153` (`targetState = !WelcomeScreenStateHolder.shouldShowMoreInfo.value`) and `:161`
(`WelcomeScreenStateHolder.shouldShowMoreInfo.value = shouldShowMoreInfoState`). The Composable cannot
be previewed or reasoned about from its parameters, and the interface extends `StateHolder` only to be a namespace for
`areResourcesLoaded()` (read by `ResourceLoader.kt:47`). `WelcomeScreen` (:63-197) itself is a ~135-line body with two distinct groups: the expandable details
(:78-145) and the "More details / Hide details" toggle (:146-196).

## Fix
1. `WelcomeScreen(modifier, shouldUseCompactUi, isMoreInfoVisible: Boolean, onMoreInfoVisibilityChanged: (Boolean) -> Unit, scrollToTop)`;
   both call sites in `HomeContent` (`ShowcaseContent.kt:392` and `:413`, the expanded and the compact welcome) pass the
   state.
2. Extract `private fun MoreInfo(...)` (the `Column { AnimatedVisibility { … } }` group) and
   `private fun MoreInfoToggle(...)` (the `AnimatedVisibility { AnimatedContent { Row { … } } }` group) with the parameters
   they read; no wrapper added or dropped.
3. Replace `WelcomeScreenStateHolder` with a plain `internal object WelcomeScreenResources { @Composable fun areResourcesLoaded() … }`
   (file renamed to match); `ResourceLoader` calls it.

## Decision
Where `shouldShowMoreInfo` lives: (a) in `ShowcaseSession` (A51), process-scoped like today; (b) a
`rememberSaveable` in `HomeContent` — survives Activity recreation but resets when the process dies, and is shared by
the two welcome instances only if hoisted above the `Crossfade`; (c) keep a file-level `mutableStateOf` in
`ShowcaseContent.kt` passed down. **Recommended: (a)** — same lifetime as today, no global read in a leaf.

## Behaviour
Same visibility rules and the same toggle; the state keeps its current lifetime under (a). Rendered UI unchanged.

## Public API
None.

## Tests
None beyond the existing ones (Composable structure; the visibility rule `!shouldUseCompactUi || isMoreInfoVisible` is
one expression — extract it as an `internal` function with a test only if it grows).

## Verify
`./gradlew :app:shared:compileKotlinDesktop :app:shared:desktopTest`

## Manual check
Compact layout: expand "More details", open an example, come back — still expanded (as today); widen the window — the
expanded layout always shows the details; on Android rotate the device — the state survives.
