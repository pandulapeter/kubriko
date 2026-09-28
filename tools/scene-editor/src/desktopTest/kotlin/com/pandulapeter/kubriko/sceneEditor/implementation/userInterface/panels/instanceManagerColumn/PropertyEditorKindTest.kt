/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn

import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PropertyEditorKindTest {

    private class TestClass {
        var a: String = ""
        var b: String? = null
        var c: Int = 0
        var d: Int? = null
        var e: Color = Color.Red
        var f: SceneOffset = SceneOffset.Zero
    }

    @Test
    fun stringMatchesRegardlessOfNullability() {
        assertEquals(PropertyEditorKind.STRING, TestClass::a.returnType.toPropertyEditorKind())
        assertEquals(PropertyEditorKind.STRING, TestClass::b.returnType.toPropertyEditorKind())
    }

    @Test
    fun otherTypesMatchExactly() {
        assertEquals(PropertyEditorKind.INT, TestClass::c.returnType.toPropertyEditorKind())
        assertNull(TestClass::d.returnType.toPropertyEditorKind())
        assertEquals(PropertyEditorKind.COLOR, TestClass::e.returnType.toPropertyEditorKind())
        assertEquals(PropertyEditorKind.SCENE_OFFSET, TestClass::f.returnType.toPropertyEditorKind())
    }
}
