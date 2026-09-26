package io.github.jamal_wia.decomposenavigator

import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import kotlin.random.Random.Default.nextLong

fun randomId(): Long = nextLong(Long.MIN_VALUE, Long.MAX_VALUE)

private val EMPTY_STACK_ANIMATION = StackAnimation<Any, Any> { stack, _, content ->
    content(stack.active)
}

@Suppress("UNCHECKED_CAST")
fun <C : Any, T : Any> emptyStackAnimation(): StackAnimation<C, T> =
    EMPTY_STACK_ANIMATION as StackAnimation<C, T>
