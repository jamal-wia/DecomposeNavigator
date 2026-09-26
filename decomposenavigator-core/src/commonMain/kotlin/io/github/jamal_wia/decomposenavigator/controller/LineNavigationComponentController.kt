package io.github.jamal_wia.decomposenavigator.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import io.github.jamal_wia.decomposenavigator.ReadinessAwaitable
import io.github.jamal_wia.decomposenavigator.ReturnsToFirstScreen
import io.github.jamal_wia.decomposenavigator.RenderComponent
import io.github.jamal_wia.decomposenavigator.StackStateSerializer
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.base.LocalNavigator
import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator
import io.github.jamal_wia.decomposenavigator.navigator.impl.LineNavigator
import io.github.jamal_wia.decomposenavigator.navigator.util.rememberNavigator
import io.github.jamal_wia.decomposenavigator.logging.Logger
import io.github.jamal_wia.decomposenavigator.logging.NoopLogger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
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
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.statekeeper.SerializableContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class LineNavigationComponentController(
    override val hostConfig: NavigationScreenConfig.LineNavigation,
    private val componentContext: ComponentContext,
    private val childFactory: (config: ScreenConfig, ctx: ComponentContext) -> RenderComponent,
    private val json: Json,
    private val logger: Logger = NoopLogger,
) : NavigationComponentController<NavigationScreenConfig.LineNavigation>(),
    RenderComponent,
    ReadinessAwaitable,
    ReturnsToFirstScreen,
    ComponentContext by componentContext {

    private val initialConfigs: List<ScreenConfig>
        get() = hostConfig.initialConfigs

    init {
        require(hostConfig.initialConfigs.isNotEmpty()) {
            "initialConfigs must not be empty."
        }
    }

    override val activeScreenConfig: ScreenConfig
        get() = childStack.active.configuration

    /**
     * Forwards readiness-awaiting to whichever screen is on top of this stack, so a caller waiting
     * for a tab to be ready reaches the screen and not the stack holding it. A stack wrapping a
     * tab's content sits between the two, and without this the wait would return at once and the
     * caller would talk to a screen that has not subscribed to anything yet.
     */
    override suspend fun awaitReady() {
        (childStack.active.instance as? ReadinessAwaitable)?.awaitReady()
    }

    private val navigation = StackNavigation<ScreenConfig>()

    // Decompose navigation drives Compose state and synchronously cascades lifecycle
    // destruction (which on Android touches the Activity window). It must run on the
    // main thread regardless of which dispatcher the caller is on.
    private val mainScope: CoroutineScope =
        coroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val childStack: Value<ChildStack<ScreenConfig, RenderComponent>> = childStack(
        source = navigation,
        initialStack = { initialConfigs },
        saveStack = { stack: List<ScreenConfig> ->
            StackStateSerializer.saveStack(stack, json, logger)
        },
        restoreStack = { container: SerializableContainer ->
            StackStateSerializer.restoreStack(container, json, logger) { initialConfigs }
        },
        key = "$typeId$id",
        handleBackButton = true,
        childFactory = childFactory
    )

    override fun navigate(
        transformer: (stack: List<ScreenConfig>) -> List<ScreenConfig>,
        onComplete: (
            newStack: List<ScreenConfig>,
            oldStack: List<ScreenConfig>
        ) -> Unit,
    ) {
        runOnMainImmediate { navigation.navigate(transformer, onComplete) }
    }

    // Decompose marks the raw `push` delicate because it happily stacks a duplicate config.
    // That is exactly what this overload is for — [pushNew] is the de-duplicating variant.
    @OptIn(DelicateDecomposeApi::class)
    fun push(config: ScreenConfig, onComplete: () -> Unit = {}) {
        runOnMainImmediate { navigation.push(config, onComplete) }
    }

    fun pushNew(config: ScreenConfig, onComplete: (isSuccess: Boolean) -> Unit = {}) {
        runOnMainImmediate { navigation.pushNew(config, onComplete) }
    }

    fun pop(onComplete: (isSuccess: Boolean) -> Unit = {}) {
        runOnMainImmediate { navigation.pop(onComplete = onComplete) }
    }

    fun replaceCurrent(config: ScreenConfig, onComplete: () -> Unit = {}) {
        runOnMainImmediate { navigation.replaceCurrent(config, onComplete) }
    }

    fun replaceAll(config: ScreenConfig, onComplete: () -> Unit = {}) {
        runOnMainImmediate { navigation.replaceAll(config, onComplete = onComplete) }
    }

    /**
     * Puts [config] on top, moving the copy already in the stack rather than adding a second one.
     *
     * [pushNew] only compares against the *top*, and Decompose rejects a stack that holds one
     * configuration twice — from `mainScope`, where the throw is nobody's to catch. So a target that
     * may already be somewhere in the back stack has to come through here.
     */
    fun pushToFront(config: ScreenConfig, onComplete: () -> Unit = {}) {
        runOnMainImmediate { navigation.pushToFront(config, onComplete = onComplete) }
    }

    /**
     * Pops to the bottom of this stack and then asks whatever is there to do the same, so a screen
     * pushed inside a tab is cleared as well. Ordered, not both at once: the child to forward to is
     * the one left standing *after* the pop, not the one that was on top before it.
     */
    override fun returnToFirstScreen() {
        popToFirst { (childStack.active.instance as? ReturnsToFirstScreen)?.returnToFirstScreen() }
    }

    fun popToFirst(onComplete: (isSuccess: Boolean) -> Unit = {}) {
        runOnMainImmediate { navigation.popToFirst(onComplete) }
    }

    fun bringToFront(config: ScreenConfig, onComplete: () -> Unit = {}) {
        runOnMainImmediate { navigation.bringToFront(config, onComplete) }
    }

    fun popTo(predicate: (ScreenConfig) -> Boolean): Boolean {
        val stackSnapshot: ChildStack<ScreenConfig, RenderComponent> = childStack.value
        val config: List<ScreenConfig> = buildList(
            capacity = stackSnapshot.backStack.size + 1
        ) {
            addAll(stackSnapshot.backStack.map { it.configuration })
            add(stackSnapshot.active.configuration)
        }

        val index: Int = config.indexOfLast(predicate)
        return if (index != -1) {
            runOnMainImmediate { navigation.popTo(index) }
            true
        } else {
            false
        }
    }

    /**
     * Dispatches [block] to the main thread. With [Dispatchers.Main.immediate], when the
     * caller already runs on main, [block] executes synchronously inline; otherwise it is
     * posted to the main thread.
     */
    private fun runOnMainImmediate(block: () -> Unit) {
        mainScope.launch { block() }
    }

    fun canPop(): Boolean {
        return childStack.value.backStack.isNotEmpty()
    }

    @Composable
    override fun Render() {
        key(typeId, id) { RenderLineNavigationComponentController() }
    }

    @Composable
    private fun RenderLineNavigationComponentController() {
        val parentNavigator: Navigator<*>? = LocalNavigator.current
        val lineNavigator: LineNavigator = rememberNavigator(
            parentNavigator = parentNavigator,
            navigationComponent = this,
            navigatorFactory = {
                LineNavigator.DefaultLineNavigator()
            }
        )

        CompositionLocalProvider(LocalNavigator provides lineNavigator) {
            Children(
                stack = childStack,
                animation = lineNavigator.animation,
            ) { child ->
                child.instance.Render()
            }
        }
    }
}
