# Move SmallSliderWithTitle into SmallSliderWithTitle.kt, keeping the SmallSliderKt JVM facade

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** tool-ui-components (published)
**Challenged:** amended — commonMain does not default-import `kotlin.jvm`, so both files also import `kotlin.jvm.JvmName` and `kotlin.jvm.JvmMultifileClass` (as P04 does); found by the orchestrator after challenger 1 caught it in P04.
**Files:** tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/SmallSlider.kt, tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/SmallSliderWithTitle.kt (new)

## Problem
`SmallSlider.kt` holds two public UI Composables, `SmallSlider` and `SmallSliderWithTitle` (lines 77-109 at 2480325f, with its
KDoc). The code style puts each non-private UI Composable in a file named after it. Both are public in a published module and
compile into the JVM facade `com.pandulapeter.kubriko.uiComponents.SmallSliderKt` (Android and desktop), which consumers built
against the current release link to (`examples/demo-particles` and `examples/demo-shader-animations` call both; Tesselar uses
neither).

## Fix
- Move `SmallSliderWithTitle` with its KDoc verbatim into `tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/SmallSliderWithTitle.kt` (MPL-2.0 header; imports `Arrangement`,
  `Row`, `defaultMinSize`, `MaterialTheme`, `Text`, `Composable`, `Alignment`, `Modifier`, `dp`); trim the imports
  `SmallSlider.kt` no longer uses (`Arrangement`, `Row`, `defaultMinSize`, `Text`, `Alignment` — check each).
- Put, between the license header and the `package` line of **both** files:
  ```kotlin
  @file:JvmName("SmallSliderKt")
  @file:JvmMultifileClass
  ```
  and add `import kotlin.jvm.JvmMultifileClass` and `import kotlin.jvm.JvmName` to both files' imports (`kotlin.jvm` is
  default-imported only on the JVM), so both functions stay on the `SmallSliderKt` facade. Both annotations are `@OptionalExpectation` in the common stdlib
  (`kotlin.jvm`), so they are allowed in `commonMain` and ignored on iOS and Wasm. No file in this repo uses
  `@JvmMultifileClass` yet — **drop this plan if the annotations don't compile on all targets** (run every compile below).
- Grep the repo for `SmallSlider.kt` in docs/CLAUDE.md files (`tools/ui-components/CLAUDE.md` lists components by name, not
  file; nothing to change expected).

## Behaviour
Verbatim move; call sites (same package import) unchanged.

## Public API
None: same package, same signatures, same JVM facade class `SmallSliderKt`.

## Tests
The existing ones.

## Verify
`./gradlew :tools:ui-components:compileKotlinDesktop :tools:ui-components:compileAndroidMain :tools:ui-components:compileKotlinWasmJs :tools:ui-components:compileKotlinIosSimulatorArm64 :examples:demo-particles:compileKotlinDesktop`,
then confirm the facade: `unzip -l` the desktop jar (`tools/ui-components/build/libs/*desktop*.jar` after
`:tools:ui-components:desktopJar`) lists `com/pandulapeter/kubriko/uiComponents/SmallSliderKt.class` and no `SmallSliderWithTitleKt.class`.

## Manual check
None.
