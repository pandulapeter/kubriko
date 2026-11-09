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

## Missing checks

- **Plain and batched drawing compared in Creator:** a generated game's Scene drawn with batched looks and drawn plain, compared as
  images in the end-to-end harness under Xvfb, with a tolerance for edge smoothing on ellipses (the engine's own comparison is
  `BatchedRenderingComparisonTest`).

## Order

As listed.
