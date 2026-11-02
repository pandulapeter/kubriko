/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

/**
 * The [steps] of one template file, in version order, and what to do when it is missing.
 *
 * @param deletion The deletion of this file by a template version that no longer has it, if any.
 */
internal class UpgradedFile(
    val path: (TemplateContext) -> String,
    val steps: List<TemplateStep>,
    val whenMissing: WhenMissing,
    val deletion: TemplateDeletion? = null,
) {
    enum class WhenMissing {
        /**
         * The file is listed for the user to bring back.
         */
        BY_HAND,

        /**
         * The file isn't needed, so there is nothing to upgrade.
         */
        SKIP,

        /**
         * The file is upgraded as if it were empty, which creates it.
         */
        CREATE_EMPTY,
    }
}
