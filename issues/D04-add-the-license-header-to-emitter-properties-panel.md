# Add the missing MPL-2.0 license header to `EmitterPropertiesPanel.kt`.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ui/EmitterPropertiesPanel.kt`

## Problem
`examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ui/EmitterPropertiesPanel.kt` starts directly with `package com.pandulapeter.kubriko.demoParticles.implementation.ui` (line 1). Every source file must start with the MPL-2.0 header; a scan of every `.kt`/`.xml`/`.kts` file under `examples/demo-*` and `examples/test-*` finds this file as the only one without it.

## Fix
Copy the 9-line header block (`/*\n * This file is part of Kubriko.\n * Copyright (c) Pandula Péter 2025-2026.\n …\n */`) verbatim from a sibling such as `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ParticlesDemoStateHolder.kt` to the top of the file. Nothing else changes.

## Behaviour
Comment only.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:demo-particles:compileKotlinDesktop`

## Manual check
none
