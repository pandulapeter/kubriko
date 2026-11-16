/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */

// Rewrites skiko.mjs so its exports do not tail-call into Wasm (WebKit bug 329421, Safari 27.2 white page).
// __dirname is build/wasm/packages/<package> at the repository root.
config.module.rules.push({
    test: /[\\/]skiko\.mjs$/,
    loader: require("path").resolve(__dirname, "../../../../app/web/webpack/skiko-no-tail-call-loader.cjs"),
});
