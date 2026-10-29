<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Later

- **Code editor (minor, like GameMaker's code action):** Kotlin syntax highlighting, search, tabs, compiler errors inline at their line, and
  "Open in IntelliJ". A language server (`kotlin-lsp`) only if there's demand.
- **Asset editor extension point:** a project or plugin registers an editor for its own resource type, running with the project's classes
  (Tesselar's model and region editors would be the first; both are already desktop modules in Tesselar, see `11-project-upkeep.md`).
  A library change, so it waits for a release.
