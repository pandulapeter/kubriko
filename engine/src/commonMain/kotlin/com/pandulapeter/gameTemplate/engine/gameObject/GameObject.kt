package com.pandulapeter.gameTemplate.engine.gameObject

abstract class GameObject<T : GameObject<T>> {
    var isSelectedInEditor = false

    abstract fun getState(): State<T>

    interface State<T : GameObject<T>> {

        val typeId: String

        fun instantiate(): T

        fun serialize(): String
    }
}