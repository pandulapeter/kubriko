package com.pandulapeter.gameTemplate.engine.gameObject

abstract class GameObject(
    val typeId: String,
    val isUnique: Boolean = false,
) {
    var isSelectedInEditor = false

    abstract fun saveState(): String
}