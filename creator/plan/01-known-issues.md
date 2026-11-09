<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Known issues

Problems found in review that need no other plan, fixed before any new feature. Each one gets its fix, a test that would have
caught it, and the review (`README.md`).

## Mistakes and failures in the first hour

What someone who knows neither Kotlin nor Gradle meets when something goes wrong:

- **The git panel** puts the commit form below every change (`ui/git/GitPanel.kt`), out of sight in a big change set. Fix: put the
  form first.
- **Undoing an Actor's removal** clears the Scene editor's selection. Fix: select the Actor again, as placing one does.
- **The audio Blocks' file menus** are empty when the project has no sounds or music. Fix: say how to add some instead.

## Missing checks

- **Plain and batched drawing compared in Creator:** a generated game's Scene drawn with batched looks and drawn plain, compared as
  images in the end-to-end harness under Xvfb, with a tolerance for edge smoothing on ellipses (the engine's own comparison is
  `BatchedRenderingComparisonTest`).

## Order

As listed: the first hour's mistakes, then the missing checks.
