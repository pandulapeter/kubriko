/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation

internal val ShowcaseEntry?.hasDebugMenu get() = this != null && this.type != ShowcaseEntryType.OTHER

internal val ShowcaseEntry?.shouldShowInfoButton get() = this?.type == ShowcaseEntryType.DEMO || this?.type == ShowcaseEntryType.TEST

internal val ShowcaseEntry?.shouldShowLogo get() = this == null || this == ShowcaseEntry.ABOUT || this == ShowcaseEntry.LICENSES
