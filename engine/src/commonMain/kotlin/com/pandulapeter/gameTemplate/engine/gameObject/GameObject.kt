package com.pandulapeter.gameTemplate.engine.gameObject

interface GameObject<O : GameObject<O>> {

    fun saveState(): State<O>
}