package com.jamal_aliev.decompose_navigator

import androidx.compose.runtime.Composable

interface RenderComponent {

    val typeId: String
    val id: Long

    @Composable
    fun Render()

}
