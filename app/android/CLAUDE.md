<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# app/android — Android entry point

Single-Activity Android app that hosts the shared `KubrikoShowcase` Composable.

## Entry point

`KubrikoShowcaseActivity : ComponentActivity` — the only Activity in the app.

Key setup in `onCreate`:
- `installSplashScreen()` with a custom zoom-out + fade exit animation via `setOnExitAnimationListener`.
- `enableEdgeToEdge()` — full bleed rendering; `WindowInsets.safeDrawing` in the shared UI handles insets.
- Fullscreen mode is a file-level `MutableStateFlow<Boolean>`. When `true`, system bars are hidden with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`; when `false`, they are restored. The flow is observed via `lifecycleScope` so the window controller update runs outside of composition.
- Deep links and back navigation are handled entirely by the shared `KubrikoShowcase` Composable.

## Build configuration

- `minSdk` / `compileSdk` / `targetSdk` — from `libs.versions` in the version catalog.
- Debug build: `applicationIdSuffix = ".debug"`, signed with the bundled `internal.keystore` (password: `android`).
- Release build: minification and resource shrinking enabled; signing credentials come from `gradle.properties` (public fallback uses `internal.keystore`). Override privately by adding any of the four keys to the gitignored `local.properties`: `showcase.androidKeyAlias`, `showcase.androidKeyPassword`, `showcase.androidKeystoreFile`, `showcase.androidKeystorePassword`.
- ProGuard rules in `proguard-rules.pro`.
- Compile/target JVM: **17** (not 21; the Android toolchain uses 17).

## Dependencies

Only `projects.app.shared`, `androidx.activity.compose`, `androidx.core.splashScreen`, and `google.material`. All game/engine logic lives in `app/shared`.
