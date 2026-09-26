package io.github.jamal_wia.decomposenavigator.navigator.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator

@Composable
internal inline fun <C : NavigationComponentController<*>, reified T : Navigator<C>> rememberNavigator(
    parentNavigator: Navigator<*>?,
    navigationComponent: C,
    crossinline navigatorFactory: () -> T
): T {
    val navigator: T = remember(parentNavigator) {
        navigatorFactory.invoke().also { newNavigator ->
            if (parentNavigator != null) {
                newNavigator.setParent(parentNavigator)
                parentNavigator.addChild(newNavigator)
            }
        }
    }

    DisposableEffect(navigator, navigationComponent) {
        navigator.bind(navigationComponent)
        onDispose { /* unbind happens on next bind or release */ }
    }

    DisposableEffect(navigator) {
        onDispose {
            navigator.release()
        }
    }

    return navigator
}
