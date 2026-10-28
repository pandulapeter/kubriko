/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class SceneSettingsCodeGeneratorTest {
    private val code = SceneSettingsCodeGenerator.generate("com.example.game")

    @Test
    fun theSettingsAreAUniqueEditableActorPlacedLikeAnyOther() {
        assertTrue(GeneratedFile.isUnchanged(code))
        assertContains(code, "package com.example.game.generated\n")
        assertContains(code, "class SceneSettings private constructor(state: State) : Visible, Unique, Editable<SceneSettings> {\n")
        assertContains(code, "EditableMetadata.create<SceneSettings, State>(typeId = \"SceneSettings\") { position ->\n")
        assertContains(code, "initialSize = SceneSize(1280f.sceneUnit, 720f.sceneUnit)")
    }

    @Test
    fun theBackgroundIsDrawnBehindEveryOtherActorOverTheBounds() {
        assertContains(code, "    override val drawingOrder = Float.MAX_VALUE\n")
        assertContains(code, "    override fun DrawScope.draw() = drawRect(\n        color = backgroundColor,\n        size = body.size.raw,\n    )\n")
        assertContains(code, "    val topLeft get() = body.axisAlignedBoundingBox.min\n")
        assertContains(code, "    val bottomRight get() = body.axisAlignedBoundingBox.max\n")
    }

    @Test
    fun theCameraMovesOnlyInTheGameAndTheSettingsRegisterWhileTheyAreInTheScene() {
        assertContains(code, "        generatedScenes.settings = this\n        if (!generatedScenes.isInSceneEditor) kubriko.get<ViewportManager>().setCameraPosition(cameraStart)\n")
        assertContains(code, "        if (generatedScenes.settings === this) generatedScenes.settings = null\n")
    }

    @Test
    fun everyValueIsExposedToTheSceneEditorAndSaved() {
        assertContains(code, "    @set:Exposed(name = \"backgroundColor\")\n    var backgroundColor: Color = state.backgroundColor\n")
        assertContains(code, "    @set:Exposed(name = \"cameraStart\")\n    var cameraStart: SceneOffset = state.cameraStart\n")
        assertContains(code, "        @SerialName(\"backgroundColor\") val backgroundColor: SerializableColor = Color(0xFF202124),\n")
        assertContains(code, "        @SerialName(\"cameraStart\") val cameraStart: SerializableSceneOffset = SceneOffset.Zero,\n")
        assertContains(code, "    @set:Exposed(name = \"music\")\n    var music: String = state.music\n")
        assertContains(code, "        @SerialName(\"music\") val music: String = \"\",\n")
        assertContains(code, "        music = music,\n    )\n")
    }
}
