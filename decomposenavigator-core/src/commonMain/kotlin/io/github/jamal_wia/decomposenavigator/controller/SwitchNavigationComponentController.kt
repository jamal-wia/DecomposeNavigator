package io.github.jamal_wia.decomposenavigator.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import io.github.jamal_wia.decomposenavigator.RenderComponent
import io.github.jamal_wia.decomposenavigator.ReturnsToFirstScreen
import io.github.jamal_wia.decomposenavigator.StackStateSerializer
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig.SwitchScreenConfigContainer
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import io.github.jamal_wia.decomposenavigator.navigator.base.LocalNavigator
import io.github.jamal_wia.decomposenavigator.navigator.base.Navigator
import io.github.jamal_wia.decomposenavigator.navigator.impl.SwitchNavigator
import io.github.jamal_wia.decomposenavigator.navigator.util.rememberNavigator
import io.github.jamal_wia.decomposenavigator.logging.Logger
import io.github.jamal_wia.decomposenavigator.logging.NoopLogger
import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class SwitchNavigationComponentController(
    override val hostConfig: NavigationScreenConfig.SwitchScreen,
    private val componentContext: ComponentContext,
    private val childFactory: (config: ScreenConfig, ctx: ComponentContext) -> RenderComponent,
    private val json: Json,
    private val logger: Logger = NoopLogger,
) : NavigationComponentController<NavigationScreenConfig.SwitchScreen>(),
    RenderComponent,
    ReturnsToFirstScreen,
    ComponentContext by componentContext {

    private val initialConfig: SwitchScreenConfigContainer get() = hostConfig.initialConfig

    private val navigation = StackNavigation<SwitchScreenConfigContainer>()

    // Decompose StackNavigation and MutableValue writes drive Compose state and may
    // synchronously cascade into Activity window mutations. Marshal every public
    // mutation onto the main thread so callers from any dispatcher are safe.
    private val mainScope: CoroutineScope =
        coroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val activeScreenConfig: ScreenConfig
        get() = childStack.active.configuration

    /**
     * A switch has no stack of its own to unwind — the tab standing in it may, so the request goes
     * to the open tab. Deliberately does *not* switch tabs: which one is open is the reader's, and
     * the caller asked to clear what covers it, not to choose it for them.
     */
    override fun returnToFirstScreen() {
        (childStack.active.instance as? ReturnsToFirstScreen)?.returnToFirstScreen()
    }

    val childStack: Value<ChildStack<SwitchScreenConfigContainer, RenderComponent>> =
        childStack(
            source = navigation,
            initialStack = { listOf(initialConfig) },
            saveStack = { stack -> StackStateSerializer.saveStack(stack, json, logger) },
            restoreStack = { container ->
                StackStateSerializer.restoreStack(container, json, logger) { listOf(initialConfig) }
            },
            key = "$typeId$id",
            handleBackButton = false,
            childFactory = childFactory
        )

    private val backStackStateKey: String = "switch_back_stack_${typeId}_$id"

    val backStack: MutableValue<List<SwitchScreenConfigContainer>> =
        MutableValue(
            runCatching {
                val jsonStr: String = stateKeeper.consume(
                    key = backStackStateKey,
                    strategy = String.serializer()
                ).orEmpty()
                StackStateSerializer.decodeFromString(
                    jsonStr = jsonStr,
                    json = json,
                    logger = logger,
                    fallback = { listOf(initialConfig) }
                )
            }.getOrElse {
                listOf(initialConfig)
            }
        )

    /**
     * Whether back should be handled by this switch container: there is a previous entry to
     * return to (`size > 1`), or the single remaining entry is not the initial tab (so back
     * should fall back to the initial tab).
     */
    private fun hasSwitchBackHistory(stack: List<SwitchScreenConfigContainer>): Boolean =
        stack.size > 1 || (stack.size == 1 && stack.last().id != initialConfig.id)

    init {
        stateKeeper.register(
            key = backStackStateKey,
            strategy = String.serializer(),
            supplier = {
                StackStateSerializer.encodeToString(backStack.value, json)
            }
        )

        var backHandlerSubscribeCancellation: Cancellation? = null

        val backCallback = BackCallback {
            val mutableBackStack: MutableList<SwitchScreenConfigContainer> = backStack.value.toMutableList()
            if (mutableBackStack.size > 1) {
                mutableBackStack.removeAt(mutableBackStack.lastIndex)
                backStack.value = mutableBackStack
                switchTo(mutableBackStack.last())
            } else if (mutableBackStack.size == 1 &&
                mutableBackStack.last().id != initialConfig.id
            ) { // fallback to root
                backStack.value = listOf(initialConfig)
                switchTo(initialConfig)
            }
        }

        fun subscribeBackStack() {
            backHandlerSubscribeCancellation?.cancel()
            backHandlerSubscribeCancellation =
                backStack.subscribe { currentBackStack: List<SwitchScreenConfigContainer> ->
                    if (hasSwitchBackHistory(currentBackStack)) {
                        if (!backHandler.isRegistered(backCallback)) {
                            backHandler.register(backCallback)
                        }
                    } else {
                        if (backHandler.isRegistered(backCallback)) {
                            backHandler.unregister(backCallback)
                        }
                    }
                }
        }

        fun unsubscribeBackStack() {
            backHandlerSubscribeCancellation?.cancel()
            backHandlerSubscribeCancellation = null
            if (backHandler.isRegistered(backCallback)) {
                backHandler.unregister(backCallback)
            }
        }

        // Registration is intentionally tied to RESUMED, not just to `backCallback.isEnabled`.
        // Essenty's BackDispatcher picks the last-registered enabled callback among those with
        // the highest priority. This switch's callback is registered AFTER the ancestor stack's
        // pop callback, so with equal (default) priority it would win even while a screen is
        // pushed on top of the tab host. Unregistering on pause (when the host leaves the top of
        // the stack) lets the ancestor's pop handle back first; re-registering on resume restores
        // tab-switch back behavior. (A priority-based alternative would avoid the lifecycle
        // coupling but needs an instrumented BackDispatcher test to verify precedence.)
        lifecycle.subscribe(
            object : Lifecycle.Callbacks {
                override fun onResume() {
                    subscribeBackStack()
                }

                override fun onPause() {
                    unsubscribeBackStack()
                }
            }
        )

        // If lifecycle is already RESUMED at the time of creation, subscribe immediately
        if (lifecycle.state >= Lifecycle.State.RESUMED) {
            subscribeBackStack()
        }

        doOnDestroy {
            unsubscribeBackStack()
        }
    }

    override fun navigate(
        transformer: (stack: List<ScreenConfig>) -> List<ScreenConfig>,
        onComplete: (newStack: List<ScreenConfig>, oldStack: List<ScreenConfig>) -> Unit
    ) {
        runOnMainImmediate {
            navigation.navigate(
                transformer = { oldStack ->
                    val transformed: List<ScreenConfig> = transformer(oldStack)
                    transformed.map { config ->
                        config as? SwitchScreenConfigContainer
                            ?: error(
                                "SwitchNavigationComponentController.navigate() requires all configs " +
                                        "to be SwitchScreenConfigContainer, but got: ${config::class.simpleName}"
                            )
                    }
                },
                onComplete = { newStack, oldStack ->
                    onComplete(newStack, oldStack)
                }
            )
        }
    }

    fun switchTo(switchConfig: SwitchScreenConfigContainer, onComplete: () -> Unit = {}) {
        runOnMainImmediate {
            val mutableStack: MutableList<SwitchScreenConfigContainer> = backStack.value.toMutableList()
            val index: Int = mutableStack.indexOfFirst { it.id == switchConfig.id }
            if (index != -1) mutableStack.removeAt(index)
            mutableStack.add(switchConfig)
            val cap: Int? = hostConfig.maxBackStackSize
            backStack.value = if (cap != null && mutableStack.size > cap) {
                mutableStack.takeLast(cap)
            } else {
                mutableStack
            }

            val current: ScreenConfig = childStack.value.active.configuration
            if (current is SwitchScreenConfigContainer && current.id == switchConfig.id) {
                // Tapping the tab you are already on takes you back to its first screen. The
                // stack is a level down from here — a switch child is always the container, and the
                // stack it wraps is the container's own child — so unwrap before asking.
                val currentChild: RenderComponent = childStack.value.active.instance
                val stack: LineNavigationComponentController? = when (currentChild) {
                    is LineNavigationComponentController -> currentChild
                    is SwitchContainerComponentController ->
                        currentChild.child as? LineNavigationComponentController

                    else -> null
                }
                stack?.popToFirst()
                onComplete()
                return@runOnMainImmediate
            }
            navigation.navigate(
                transformer = { oldStack: List<SwitchScreenConfigContainer> ->
                    val newStack = ArrayList<SwitchScreenConfigContainer>(oldStack.size)
                    var targetConfig: SwitchScreenConfigContainer = switchConfig
                    oldStack.onEach { config: SwitchScreenConfigContainer ->
                        if (config.id == switchConfig.id) {
                            targetConfig = config
                        } else {
                            newStack.add(config)
                        }
                    }
                    newStack.apply { add(targetConfig) }
                },
                onComplete = { _, _ -> onComplete() }
            )
        }
    }

    /**
     * Pops the last entry from the back stack and switches to the new last entry.
     * Updates the back stack before switching so that the back handler state
     * stays consistent with the visible screen.
     * Returns the popped config, or `null` if the back stack has 1 or fewer entries.
     */
    fun popBackStack(): SwitchScreenConfigContainer? {
        // Snapshot is computed synchronously (read-only access to MutableValue is thread-safe);
        // mutations happen on the main thread to keep Compose/Decompose invariants.
        val currentBackStack: List<SwitchScreenConfigContainer> = backStack.value
        if (currentBackStack.size <= 1) return null
        val popped: SwitchScreenConfigContainer = currentBackStack.last()
        val newBackStack: List<SwitchScreenConfigContainer> = currentBackStack.dropLast(1)
        val target: SwitchScreenConfigContainer = newBackStack.last()
        runOnMainImmediate {
            backStack.value = newBackStack
            switchTo(target)
        }
        return popped
    }

    /**
     * Pops all entries except the first and switches to it.
     * Updates the back stack before switching so that the back handler state
     * stays consistent with the visible screen.
     * Returns the first config, or `null` if the back stack is empty.
     */
    fun popBackStackToRoot(): SwitchScreenConfigContainer? {
        val currentBackStack: List<SwitchScreenConfigContainer> = backStack.value
        if (currentBackStack.size <= 1) return currentBackStack.firstOrNull()
        val first: SwitchScreenConfigContainer = currentBackStack.first()
        runOnMainImmediate {
            backStack.value = listOf(first)
            switchTo(first)
        }
        return first
    }

    /**
     * Dispatches [block] onto the main thread. With [Dispatchers.Main.immediate], when the
     * caller is already on main, [block] runs synchronously inline; otherwise it is posted.
     */
    private fun runOnMainImmediate(block: () -> Unit) {
        mainScope.launch { block() }
    }

    @Composable
    override fun Render() {
        key(typeId, id) { RenderSwitchNavigationComponent() }
    }

    @Composable
    private fun RenderSwitchNavigationComponent() {
        val parentNavigator: Navigator<*>? = LocalNavigator.current
        val switchNavigator: SwitchNavigator = rememberNavigator(
            parentNavigator = parentNavigator,
            navigationComponent = this,
            navigatorFactory = {
                SwitchNavigator.DefaultSwitchNavigator()
            }
        )

        CompositionLocalProvider(LocalNavigator provides switchNavigator) {
            Children(
                stack = childStack,
                animation = switchNavigator.animation,
            ) { child ->
                child.instance.Render()
            }
        }
    }
}
