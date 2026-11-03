/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.actors.codegen.PluginUse
import com.pandulapeter.kubrikoCreator.project.LibraryInUse
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.project_settings_library_always
import kubriko.creator.generated.resources.project_settings_library_file
import kubriko.creator.generated.resources.project_settings_library_generated
import org.jetbrains.compose.resources.stringResource

/**
 * Why [library] is in use, in the order the section lists the reasons: what the types and Scenes do, then the files that
 * name it, then the generated code if nothing else says why.
 */
internal fun reasonsOf(library: LibraryInUse): List<LibraryReason> = when {
    library.isAlwaysUsed -> listOf(LibraryReason.Always)
    else -> library.uses.map(LibraryReason::Use) +
        library.handWrittenFiles.map(LibraryReason::File) +
        listOfNotNull(LibraryReason.Generated.takeIf { library.isUsedByGeneratedCode && library.uses.isEmpty() })
}

/**
 * One line of a library's row in [LibrariesPage].
 */
internal sealed interface LibraryReason {

    @Composable
    fun text(): String

    data object Always : LibraryReason {
        @Composable
        override fun text() = stringResource(Res.string.project_settings_library_always)
    }

    data object Generated : LibraryReason {
        @Composable
        override fun text() = stringResource(Res.string.project_settings_library_generated)
    }

    data class Use(val use: PluginUse) : LibraryReason {
        @Composable
        override fun text() = stringResource(use.text, *use.arguments.toTypedArray())
    }

    data class File(val path: String) : LibraryReason {
        @Composable
        override fun text() = stringResource(Res.string.project_settings_library_file, path)
    }
}
