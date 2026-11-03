/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Splits a UTF-8 byte stream into lines, without their line breaks.
 */
internal class LineOutputStream(
    private val onLine: (String) -> Unit,
) : OutputStream() {
    private val buffer = ByteArrayOutputStream()

    override fun write(byte: Int) {
        if (byte == '\n'.code) {
            onLine(buffer.toString(Charsets.UTF_8).trimEnd('\r'))
            buffer.reset()
        } else {
            buffer.write(byte)
        }
    }
}
