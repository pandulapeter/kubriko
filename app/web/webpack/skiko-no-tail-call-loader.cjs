/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */

// Workaround for WebKit bug 329421 (iOS / macOS Safari 27.2): a JavaScript tail call into a Wasm function
// can pass a float argument as NaN once the code is optimized. Every export of skiko.mjs is such a tail call
// (`(...a) => (X = loadedWasm._["X"])(...a)`), so Skia receives NaN sizes and coordinates and the page stays
// white over https. A call inside a `try` block is never in tail position, which avoids the broken path.
// See https://youtrack.jetbrains.com/issue/CMP-10986. Remove once the fixed Safari is widespread.
const WRAPPER = /\(\.\.\.a\) => \((\w+) = loadedWasm\._\["\1"\]\)\(\.\.\.a\)/g;

module.exports = function (source) {
    let patched = 0;
    const result = source.replace(WRAPPER, (_, name) => {
        patched++;
        return `(...a) => { try { return (${name} = loadedWasm._["${name}"])(...a) } catch (e) { throw e } }`;
    });
    if (patched === 0) {
        throw new Error("skiko.mjs wrapper format changed, the WebKit 329421 workaround was not applied");
    }
    return result;
};
