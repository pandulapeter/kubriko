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
- **Hot reload** (Compose Hot Reload in the session process) so Block and Kotlin changes apply to a running game without a restart; the
  biggest answer to Kotlin's compile times.
- **Asset editor extension point:** a project or plugin registers an editor for its own resource type, running with the project's classes
  (Tesselar's cuboid model editor would be the first).
- **Project settings panel:** targets, application ID and name, icons, Managers and Plugins (as a diff), aspect ratio, target frame rate.
- **Library version range:** which Kubriko versions Creator supports for projects that aren't opted in (opted-in ones use Creator's own,
  `00-library-release.md`).
