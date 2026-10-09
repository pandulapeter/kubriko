/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui.licenses

import kubriko.app.shared.generated.resources.Res
import kubriko.app.shared.generated.resources.other_licenses_apache_2_0
import kubriko.app.shared.generated.resources.other_licenses_cc0_1_0
import kubriko.app.shared.generated.resources.other_licenses_ccby_4_0
import kubriko.app.shared.generated.resources.other_licenses_lgpl_2_1
import kubriko.app.shared.generated.resources.other_licenses_mit
import kubriko.app.shared.generated.resources.other_licenses_mpl_2_0
import org.jetbrains.compose.resources.StringResource

internal enum class LicenseType(
    val licenseName: StringResource,
) {
    APACHE_2_0(
        licenseName = Res.string.other_licenses_apache_2_0,
    ),
    CC0_1_0(
        licenseName = Res.string.other_licenses_cc0_1_0,
    ),
    CCBY_4_0(
        licenseName = Res.string.other_licenses_ccby_4_0,
    ),
    LGPL_2_1(
        licenseName = Res.string.other_licenses_lgpl_2_1,
    ),
    MIT(
        licenseName = Res.string.other_licenses_mit,
    ),
    MPL_2_0(
        licenseName = Res.string.other_licenses_mpl_2_0,
    );
}
