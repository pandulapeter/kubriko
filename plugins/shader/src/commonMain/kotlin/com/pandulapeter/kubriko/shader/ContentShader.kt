package com.pandulapeter.kubriko.shader

//TODO: Documentation
interface ContentShader<T : Shader.State> : Shader<T> {

    companion object {
        const val CONTENT = "content"
    }
}