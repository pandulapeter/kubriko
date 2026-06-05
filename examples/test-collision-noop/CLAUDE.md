<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# test-collision-noop

Blank substitute for `examples/test-collision`, compiled into the Showcase app when
`showcase.areTestExamplesEnabled=false` in `gradle.properties`.

## What it contains

Two files that mirror the public API surface of `test-collision` but do nothing:

- `CollisionTest.kt` — `createCollisionTestStateHolder()` returns `CollisionTestStateHolderImpl()`;
  the `CollisionTest` composable renders nothing (`= Unit`).
- `CollisionTestStateHolder.kt` — `CollisionTestStateHolder` sealed interface with
  `areResourcesLoaded()` always returning `true`; `CollisionTestStateHolderImpl` holds
  `kubriko = emptyFlow<Kubriko?>()` and a no-op `dispose()`.

## Rules for editing

- The public function/class/interface names and signatures must stay identical to `test-collision`.
- Do not add any real logic or dependencies here.
- Keep `areResourcesLoaded()` returning `true` unconditionally so the Showcase loading
  gate never blocks on this module.
