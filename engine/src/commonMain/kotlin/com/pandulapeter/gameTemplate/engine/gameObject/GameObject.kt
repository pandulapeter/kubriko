package com.pandulapeter.gameTemplate.engine.gameObject

abstract class GameObject<T : GameObject<T>>(
    val typeId: String,
    val isUnique: Boolean = false, // TODO: Should be a trait instead
) {
    var isSelectedInEditor = false

    abstract fun getState(): State<T>

    interface State<T : GameObject<T>> {

        fun instantiate(): T

        fun serialize(): String
    }
}