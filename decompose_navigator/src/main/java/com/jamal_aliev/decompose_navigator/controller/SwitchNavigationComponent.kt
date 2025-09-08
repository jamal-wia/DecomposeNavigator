package com.jamal_aliev.decompose_navigator.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.jamal_aliev.decompose_navigator.RenderComponent
import com.jamal_aliev.decompose_navigator.config.DecomposeNavigationConfig
import com.jamal_aliev.decompose_navigator.navigator.LocalNavigator
import com.jamal_aliev.decompose_navigator.NavigationComponent
import com.jamal_aliev.decompose_navigator.navigator.Navigator
import com.jamal_aliev.decompose_navigator.navigator.impl.SwitchNavigator

class SwitchNavigationComponent(
    override val typeId: String,
    override val id: Long,
    componentContext: ComponentContext,
    initialConfig: DecomposeNavigationConfig.SwitchContainer,
    private val childFactory: (config: DecomposeNavigationConfig, ctx: ComponentContext) -> RenderComponent,
) : ComponentContext by componentContext, RenderComponent, NavigationComponent {

    private val navigation = StackNavigation<DecomposeNavigationConfig.SwitchContainer>()

    val childStack: Value<ChildStack<DecomposeNavigationConfig.SwitchContainer, RenderComponent>> =
        childStack(
            source = navigation,
            initialStack = {
                listOf(initialConfig)
            },
            saveStack = {
                SerializableContainer()
            },
            restoreStack = {
                listOf()
            },
            key = "$typeId$id",
            handleBackButton = false,
            childFactory = childFactory
        )

    override val activeConfig: DecomposeNavigationConfig
        get() = childStack.active.configuration

    val backStack = MutableValue(listOf(initialConfig))

    init {
        val backCallback = BackCallback {
            val mutableBackStack = backStack.value.toMutableList()
            mutableBackStack.removeAt(mutableBackStack.lastIndex)
            backStack.value = mutableBackStack
            switchTo(mutableBackStack.lastOrNull() ?: initialConfig)
        }
        var backHandlerSubscribeCancellation: Cancellation? = null

        doOnResume {
            backHandlerSubscribeCancellation = backStack.subscribe(
                fun(currentBackStack: List<DecomposeNavigationConfig.SwitchContainer>) {
                    if (currentBackStack.size > 1 ||
                        (currentBackStack.size == 1 && currentBackStack.last() != initialConfig)
                    ) {
                        if (!backHandler.isRegistered(backCallback))
                            backHandler.register(backCallback)
                    } else {
                        if (backHandler.isRegistered(backCallback))
                            backHandler.unregister(backCallback)
                    }
                }
            )
        }

        doOnPause {
            backHandlerSubscribeCancellation?.cancel()
            if (backHandler.isRegistered(backCallback))
                backHandler.unregister(backCallback)
        }
    }

    fun switchTo(switchconfig: DecomposeNavigationConfig.SwitchContainer) {
        val mutableStack = backStack.value.toMutableList()
        val index = mutableStack.indexOfFirst { it.id == switchconfig.id }
        if (index != -1) mutableStack.removeAt(index)
        mutableStack.add(switchconfig)
        backStack.value = mutableStack

        val current: DecomposeNavigationConfig? = childStack.value.active.configuration
        if (current is DecomposeNavigationConfig.SwitchContainer && current.id == switchconfig.id) {
            // повторный вызов на том же слоте
            val currentChild: RenderComponent? = childStack.value.active.instance
            if (currentChild is LineNavigationComponent) {
                currentChild.popToFirst()
            }
            return
        }
        navigation.navigate(
            transformer = { oldStack: List<DecomposeNavigationConfig.SwitchContainer> ->
                val newStack = ArrayList<DecomposeNavigationConfig.SwitchContainer>(oldStack.size)
                var targetconfig: DecomposeNavigationConfig.SwitchContainer =
                    switchconfig
                oldStack.onEach { config: DecomposeNavigationConfig.SwitchContainer ->
                    if (config.id == switchconfig.id) targetconfig =
                        config
                    else newStack.add(config)
                }
                newStack.apply { add(targetconfig) }
            },
            onComplete = { newStack, oldStack ->

            }
        )
    }

    @Composable
    override fun Render() {
        key(typeId, id) { RenderSwitchNavigationComponent() }
    }

    @Composable
    private fun RenderSwitchNavigationComponent() {
        val currentNavigator: Navigator? = LocalNavigator.current
        val switchNavigator: SwitchNavigator = remember(currentNavigator) {
            SwitchNavigator.SwitchNavigatorImpl(parent = currentNavigator)
                .also { currentNavigator?.addChild(it) }
        }

        CompositionLocalProvider(LocalNavigator provides switchNavigator) {
            val slotValue: ChildStack<DecomposeNavigationConfig.SwitchContainer, RenderComponent>
                    by childStack.subscribeAsState()

            val instance: RenderComponent = slotValue.active.instance
            key(instance.typeId, instance.id) {
                instance.Render()
            }
        }

        DisposableEffect(switchNavigator) {
            switchNavigator.bind(this@SwitchNavigationComponent)
            onDispose { switchNavigator.unbind() }
        }
    }
}
