/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class SpriteLookCodeTest {
    private val sheet = SpriteSheet(frameWidth = 20, frameHeight = 10, frameCount = 6, framesPerRow = 3, framesPerSecond = 8f)
    private val sheets = mapOf("hero-walk" to sheet)
    private val hero = ActorTypeDefinition(name = "Hero", sprite = "drawable/hero-walk.png", width = 40f, height = 20f)

    private fun generate(definition: ActorTypeDefinition = hero, spriteSheets: Map<String, SpriteSheet> = sheets, actorTypes: List<ActorTypeDefinition> = listOf(definition)) =
        ActorTypeCodeGenerator.generateActorType(definition, "com.example.game", actorTypes = actorTypes, spriteSheets = spriteSheets)

    @Test
    fun aSpriteCutIntoFramesPlaysThemEveryFrame() {
        val code = generate()
        assertContains(code, "import com.pandulapeter.kubriko.sprites.AnimatedSprite\n")
        assertContains(code, ") : Batched, Dynamic, Editable<Hero> {")
        assertContains(
            code,
            "    private val animation = AnimatedSprite.fromAtlas(\n" +
                "        getRegion = { sprite?.let { spriteManager.getRegion(it) } },\n" +
                "        frameSize = IntSize(20, 10),\n" +
                "        frameCount = 6,\n" +
                "        framesPerRow = 3,\n" +
                "        framesPerSecond = 8f,\n" +
                "    )\n",
        )
        assertContains(code, "        if (isRemoved) return\n        animation.stepForward(deltaTimeInMilliseconds, animationSpeed, isAnimationLooping)\n")
        assertContains(code, "            animation.applyTo(field)\n            field.tint = if (opacity < 1f) Color.White.copy(alpha = opacity) else Color.White\n            return field\n")
        assertFalse("setRegion" in code || "fun setFrame" in code)
    }

    @Test
    fun aSheetIsFoundByTheSpritesNameAndOnlyForAnImageRightInDrawable() {
        assertFalse("AnimatedSprite" in generate(spriteSheets = mapOf("hero_walk" to sheet)))
        assertFalse("AnimatedSprite" in generate(hero.copy(sprite = "drawable/nested/hero-walk.png")))
        assertFalse("AnimatedSprite" in generate(hero.copy(sprite = null)))
    }

    @Test
    fun theFrameBlockGetsItsFunctionAndTheAnimationBlockItsFields() {
        val code = generate(
            hero.copy(events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetFrame(NumberValue.Fixed(9f)), BlockDefinition.SetAnimation(speed = NumberValue.Fixed(2f)))))),
        )
        assertContains(code, "    private fun setFrame(frame: Int) {\n        animation.frameIndex = frame.coerceIn(0, animation.frameCount - 1)\n    }\n")
        assertContains(code, "        setFrame(9)\n        animationSpeed = 2f\n        isAnimationLooping = true\n")
        assertContains(code, "    var animationSpeed = 1f\n")
        assertContains(code, "    var isAnimationLooping = true\n")
    }

    @Test
    fun aTintedSpriteIsMultipliedByTheColorEitherAtTheOpacityAndAShapeIsntTintedTwice() {
        val tinted = "field.tint = if (opacity < 1f) color.copy(alpha = color.alpha * opacity) else color\n"
        assertContains(generate(hero.copy(tintsSprite = true)), "            animation.applyTo(field)\n            $tinted")
        assertContains(
            generate(hero.copy(tintsSprite = true), spriteSheets = emptyMap()),
            "            if (field.texture == null) field.setRegion(sprite?.let { spriteManager.getRegion(it) })\n            $tinted",
        )
        assertContains(
            generate(hero, spriteSheets = emptyMap()),
            "            if (field.texture == null) field.setRegion(sprite?.let { spriteManager.getRegion(it) })\n            field.tint = if (opacity < 1f) Color.White.copy(alpha = opacity) else Color.White\n",
        )
        val shape = generate(hero.copy(sprite = null, tintsSprite = true))
        assertFalse("tint" in shape)
        assertContains(shape, "            field.color = if (opacity < 1f) color.copy(alpha = color.alpha * opacity) else color\n")
    }

    @Test
    fun everyActorTurnsAroundTheOriginWhateverItsSize() {
        val code = generate(spriteSheets = mapOf("hero-walk" to sheet.copy(originX = 5, originY = 10)))
        assertContains(code, "    init {\n")
        assertContains(code, "        body.pivot = SceneOffset(body.size.width * 0.25f, body.size.height * 1f)\n")
        assertContains(code, "import com.pandulapeter.kubriko.types.SceneOffset\n")
        assertContains(generate(spriteSheets = mapOf("hero-walk" to sheet.copy(originY = 0))), "body.pivot = SceneOffset(body.size.width * 0.5f, body.size.height * 0f)")
        assertFalse("body.pivot" in generate())
    }

    @Test
    fun aFamilysBaseClassPlaysTheFramesOfWhicheverMemberHasThem() {
        val parent = ActorTypeDefinition(name = "Enemy", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.SetFrame(NumberValue.Fixed(1f))))))
        val child = hero.copy(name = "Bat", parent = "Enemy")
        val types = listOf(parent, child)
        val parentCode = generate(parent, actorTypes = types)
        assertContains(parentCode, "    protected open fun setFrame(frame: Int) = Unit\n")
        assertContains(parentCode, "    protected open fun stepAnimation(deltaTimeInMilliseconds: Int) = Unit\n")
        assertContains(parentCode, "        if (isRemoved) return\n        stepAnimation(deltaTimeInMilliseconds)\n")
        assertFalse("AnimatedSprite" in parentCode)
        val childCode = generate(child, actorTypes = types)
        assertContains(childCode, "    override fun setFrame(frame: Int) {\n")
        assertContains(childCode, "    override fun stepAnimation(deltaTimeInMilliseconds: Int) {\n        animation.stepForward(deltaTimeInMilliseconds, animationSpeed, isAnimationLooping)\n    }\n")
        val stillCode = generate(parent, spriteSheets = emptyMap(), actorTypes = types)
        assertContains(stillCode, "// This Block only works in an Actor type whose sprite is cut into frames.")
        assertFalse("stepAnimation" in stillCode)
    }
}
