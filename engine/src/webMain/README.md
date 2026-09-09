<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Web source set

Notes for `checkTriangleBridge.mjs`, the dependency-free check for the JavaScript embedded in
`WasmTriangleBridge.kt`. It sits here rather than under `kotlin/` so it stays out of the published
sources jar.

```bash
node engine/src/webMain/checkTriangleBridge.mjs
```

It parses the two `@JsFun` bodies straight out of the Kotlin file and runs them against fake Wasm
memories and a fake Skia export table, so it always tests the code that actually ships. It needs
nothing but Node.

## When to run it

- **Before merging any edit to the `@JsFun` bodies in `WasmTriangleBridge.kt`.** This is the main
  reason it exists: that JavaScript never sees the Kotlin compiler, so a typo in it is invisible
  until a browser silently falls back to the slow path or draws the wrong bytes.
- **After bumping Kotlin, Skiko or Compose Multiplatform.** The bridge reaches past the public API
  into Skiko's native draw export and Kotlin's generated `wasmExports` binding, and either can move
  under it. Pair this with a real browser run — see the limits below.
- **When a web build renders geometry wrongly, or the web frame rate drops back to what it was
  before the bridge existed.** Running the check first settles whether the staging arithmetic or the
  guard conditions are at fault, before anyone goes looking at GPU drivers.

Nothing else touches it, so ordinary engine work does not need it. It is not wired into `gradlew
build`, deliberately: it guards a file that changes perhaps once a year, and a Node dependency in
the build for that trade is not worth it.

## What it covers

Byte-exact staging of positions, colors, texture coordinates and indices into the target heap; the
argument list and offsets handed to the native draw; untextured batches; growth of either Wasm
memory between draws (both heap views must be re-resolved, never cached across a growth); scratch
reallocation on a larger mesh; a failed allocation reporting `false` so the caller falls back; the
`pagehide` reset; and the setup guard rejecting a module whose exports are missing or whose draw
function has the wrong arity.

## What it does not cover

- **A same-signature semantic change in Skia.** If `org_jetbrains_skia_Canvas__1nDrawVertices` keeps
  its ten parameters but changes what they mean, every assertion here still passes and the rendering
  is wrong. Only running the web build and looking at it catches that.
- **Whether the fast path is taken at runtime.** The bridge deliberately falls back to public Skiko
  whenever the exports look unfamiliar, so a broken assumption shows up as lost performance rather
  than as an error. To tell the two paths apart in a browser, set
  `globalThis.__kubrikoLegacyVertices = true` before the page initializes to force the fallback, and
  compare frame times against a normal run.
- **Anything about the GPU.** No rasterization, no driver behavior, no visual comparison.
