<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Gradle

This folder contains various components required for the build process:

- The Gradle wrapper
- The [libs.versions.toml](https://github.com/pandulapeter/kubriko/blob/main/gradle/libs.versions.toml) file that defines the version numbers of all third party
  dependencies
- The `build-logic` project which contains pre-compiled Gradle plugins to reduce code duplication across the build scripts