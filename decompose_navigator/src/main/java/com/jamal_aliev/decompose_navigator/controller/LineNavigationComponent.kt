package com.jamal_aliev.decompose_navigator.controller

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.popToFirst
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.jamal_aliev.decompose_navigator.RenderComponent
import com.jamal_aliev.decompose_navigator.config.DecomposeNavigationConfig
import com.jamal_aliev.decompose_navigator.navigator.LocalNavigator
import com.jamal_aliev.decompose_navigator.NavigationComponent
import com.jamal_aliev.decompose_navigator.navigator.Navigator
import com.jamal_aliev.decompose_navigator.navigator.impl.LineNavigator
import kotlinx.serialization.KSerializer

class LineNavigationComponent(
    override val typeId: String,
    override val id: Long,
    private val componentContext: ComponentContext,
    private val initialConfigs: List<DecomposeNavigationConfig>,
    private val childFactory: (config: DecomposeNavigationConfig, ctx: ComponentContext) -> RenderComponent,
    private val serializer: KSerializer<DecomposeNavigationConfig>? = null,
) : ComponentContext by componentContext, RenderComponent, NavigationComponent {

    private val navigation = StackNavigation<DecomposeNavigationConfig>()

    val childStack: Value<ChildStack<DecomposeNavigationConfig, RenderComponent>> = childStack(
        source = navigation,
        initialStack = { initialConfigs },
        saveStack = {
            SerializableContainer()
        },
        restoreStack = {
            listOf()
        },
        key = "$typeId$id",
        handleBackButton = true,
        childFactory = childFactory
    )

    override val activeConfig: DecomposeNavigationConfig
        get() = childStack.active.configuration

    fun push(config: DecomposeNavigationConfig) {
        navigation.push(config)
    }

    fun pushNew(config: DecomposeNavigationConfig) {
        navigation.pushNew(config)
    }

    fun pop(onComplete: (Boolean) -> Unit = {}) {
        navigation.pop(onComplete = onComplete)
    }

    fun replaceCurrent(config: DecomposeNavigationConfig) {
        navigation.replaceCurrent(config)
    }

    fun replaceAll(config: DecomposeNavigationConfig) {
        navigation.replaceAll(config)
    }

    fun popToFirst() {
        navigation.popToFirst()
    }

    fun bringToFront(config: DecomposeNavigationConfig) {
        navigation.bringToFront(config)
    }

    fun popTo(predicate: (DecomposeNavigationConfig) -> Boolean): Boolean {
        // Найдём индекс целевой конфигурации в текущем стеке
        val stackSnapshot = childStack.value
        val config: List<DecomposeNavigationConfig> = buildList(
            capacity = stackSnapshot.backStack.size + 1
        ) {
            addAll(stackSnapshot.backStack.map { it.configuration })
            add(stackSnapshot.active.configuration)
        }
        val index = config.indexOfLast { predicate(it) }
        return if (index != -1) {
            navigation.popTo(index)
            true
        } else {
            false
        }
    }

    fun canPop(): Boolean {
        val stackSnapshot = childStack.value
        val size = stackSnapshot.backStack.size + 1 // backStack + active
        return size > 1
    }

    @Composable
    override fun Render() {
        key(typeId, id) { RenderLineNavigationComponent() }
    }

    @Composable
    private fun RenderLineNavigationComponent() {
        val currentNavigator: Navigator? = LocalNavigator.current
        val lineNavigator: LineNavigator = remember(currentNavigator) {
            LineNavigator.LineNavigatorImpl(parent = currentNavigator)
                .also { currentNavigator?.addChild(it) }
        }

        CompositionLocalProvider(LocalNavigator provides lineNavigator) {
            val stackValue: ChildStack<DecomposeNavigationConfig, RenderComponent>
                    by childStack.subscribeAsState()

            val instance: RenderComponent? = stackValue.active.instance
            if (instance != null) {
                key(instance.typeId, instance.id) {
                    instance.Render()
                }
            }
            else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active slot")
                }
            }
        }

        DisposableEffect(lineNavigator) {
            lineNavigator.bind(this@LineNavigationComponent)
            onDispose { lineNavigator.unbind() }
        }
    }
}
