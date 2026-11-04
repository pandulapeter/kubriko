/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.game_module_root
import org.jetbrains.compose.resources.stringResource

/**
 * The name a module goes by where it stands alone, such as a menu of modules: its Gradle path ([gradlePath],
 * `:app:desktop`) without the leading separator (`app:desktop`), which a name never starts with, or for the root
 * project, the root module.
 */
@Composable
internal fun gradleModuleName(gradlePath: String) = gradlePath.removePrefix(":").ifEmpty { stringResource(Res.string.game_module_root) }

/**
 * The name the module in the folder [path] goes by where it is shown inside the module holding it, as the sidebar does:
 * its folder's own name (`desktop` for `app/desktop`), or for the root project, the root module.
 */
@Composable
internal fun moduleFolderName(path: String) = if (path.isEmpty()) stringResource(Res.string.game_module_root) else path.substringAfterLast('/')
