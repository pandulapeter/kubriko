<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Creator

This folder contains Kubriko Creator, a standalone desktop app for making Kubriko games without needing to write code in an IDE.

> [!WARNING]
> Kubriko Creator is in the early stages of development and has not been released yet. Expect missing features, rough edges, and breaking changes to the
> project format. Until it is ready, the [engine](https://github.com/pandulapeter/kubriko/tree/main/engine),
> [plugins](https://github.com/pandulapeter/kubriko/tree/main/plugins), and [tools](https://github.com/pandulapeter/kubriko/tree/main/tools) remain the
> recommended way to build games with Kubriko.

## Overview

- Actor types and Manager types are built from Events and Blocks, then generated into plain Kotlin code that stays readable and editable.
- Actors are arranged in Scenes through an embedded scene editor.
- Games can be played with a single click, while their logs stream back into the editor.
- Projects are versioned with built-in git support.
- An optional AI assistant can help with building the game.

## Running it

Kubriko Creator is not distributed as a download yet, so it has to be run from this repository:

```bash
./gradlew :creator:run
```

This publishes the repository's own libraries to Maven Local first, so the projects created with it use them.

## Modules

The app itself lives in this folder, and its functionality is split into the Gradle modules found next to it (such as `core`, `workspace`, `ai`, and `git`).
Unlike the rest of the repository, none of them are published as libraries.
