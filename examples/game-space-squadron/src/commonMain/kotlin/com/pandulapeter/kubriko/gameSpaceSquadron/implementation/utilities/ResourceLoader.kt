/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.utilities

/**
 * The historical navigation of the Showcase app breaks web URI resource handling as it adds fake paths to the URL.
 * This messy workaround redirects the URI back to the root.
 * Most apps should not need this, as Res.getUri() should work fine under normal circumstances.
 */
internal expect fun getResourceUri(path: String, webRootPathName: String): String