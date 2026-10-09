# Split `GamepadFocusNavigation.kt` by declaration, keeping its `GamepadFocusNavigationKt` facade

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-gamepad-input
**Challenged:** amended — the two file annotations need explicit `kotlin.jvm` imports in commonMain (not default-imported there; the repo already imports `kotlin.jvm.JvmInline` the same way); the facade has shipped since 0.7.0, not 0.7.2.
**Files:**
- `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadFocusNavigation.kt`
- new `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadFocusNavigationHost.kt`
- new `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadFocusNavigationHostState.kt`
- new `plugins/gamepad-input/src/commonMain/kotlin/com/pandulapeter/kubriko/gamepadInput/GamepadActivationNode.kt`
- `plugins/gamepad-input/CLAUDE.md`

## Problem
At 2480325f one file holds five declarations:
- `@Composable fun GamepadFocusNavigationHost(gamepadInputManager, onBack)` (:50–68, public, with KDoc :27–49)
- `internal class GamepadFocusNavigationHostState(...)` (:70–75), read by `GamepadInputManager`/`GamepadInputManagerImpl`
- `fun Modifier.onGamepadActivation(gamepadInputManager, onActivation)` (:77–102, public, with KDoc)
- `internal class GamepadActivationNode(...)` (:104–140), held by `GamepadInputManager`
- `private data class GamepadActivationElement(...)` (:142–162), used only by `onGamepadActivation`

The code-style skill wants a non-private UI Composable in a file named exactly after it and each class in a file of
its own. The file has shipped since 0.7.0 (tags 0.7.0–0.7.2 contain commit 6ea5d679), so its two public top-level functions compile into
the published `GamepadFocusNavigationKt` facade and must stay there.

## Fix
Verbatim moves, same package `com.pandulapeter.kubriko.gamepadInput`, each KDoc/`//` comment with its declaration,
MPL-2.0 header copied from a sibling:
- `GamepadFocusNavigationHost` (with KDoc) → `GamepadFocusNavigationHost.kt`.
- `GamepadFocusNavigationHostState` → `GamepadFocusNavigationHostState.kt`.
- `GamepadActivationNode` (with its `//` comment on `setGamepadInputManager`, unchanged here) → `GamepadActivationNode.kt`.
- `onGamepadActivation` and the private `GamepadActivationElement` stay in `GamepadFocusNavigation.kt`.

Put these two lines at the very top of **both** `GamepadFocusNavigation.kt` and `GamepadFocusNavigationHost.kt`
(before the `package` line, after the license header):

```kotlin
@file:JvmName("GamepadFocusNavigationKt")
@file:JvmMultifileClass
```

and add to the import list of both files:

```kotlin
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
```

`kotlin.jvm.*` is default-imported only on the JVM, so commonMain does not resolve the bare names without these imports
(both annotations are `@OptionalExpectation` in the common stdlib, so they compile on every target and only take effect
on the JVM ones — desktop and Android). Alternatively spell them fully qualified (`@file:kotlin.jvm.JvmName(...)`);
the imports match how the engine's commonMain imports `kotlin.jvm.JvmInline`.

The two classes need no facade (a class's JVM name does not depend on its file). No visibility changes are needed:
everything moved is already `internal` or public in the same package. Trim imports in every file to what it uses.

`plugins/gamepad-input/CLAUDE.md` Key Files (:21) currently says
"`src/commonMain/.../GamepadFocusNavigation.kt` — the app-facing half of focus navigation: `GamepadFocusNavigationHost`
and the `onGamepadActivation` Modifier with the focus-event node behind it" — reword to name the four files. Grep the
repo for other mentions of `GamepadFocusNavigation.kt`.

Check the split mechanically (`git diff --color-moved=dimmed-zebra`).

## Behaviour
Unchanged — verbatim moves.

## Public API
None, provided both parts carry the facade annotations: `GamepadFocusNavigationKt.GamepadFocusNavigationHost(...)`
and `GamepadFocusNavigationKt.onGamepadActivation(...)` keep their JVM owner, so code compiled against 0.7.2 links.
After building, confirm with `javap -classpath plugins/gamepad-input/build/classes/kotlin/desktop/main
com.pandulapeter.kubriko.gamepadInput.GamepadFocusNavigationKt` that both functions are listed on the facade.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:gamepad-input:compileKotlinDesktop :plugins:gamepad-input:compileAndroidMain :plugins:gamepad-input:compileKotlinWasmJs :plugins:gamepad-input:desktopTest`, then the `javap` check above.

## Manual check
none
