package io.github.jamal_wia.decomposenavigator.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import io.github.jamal_wia.decomposenavigator.ReadinessAwaitable
import io.github.jamal_wia.decomposenavigator.RenderComponent
import io.github.jamal_wia.decomposenavigator.ReturnsToFirstScreen
import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.base.NavigationComponentController
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext

class SwitchContainerComponentController(
    override val hostConfig: NavigationScreenConfig.SwitchScreenConfigContainer,
    val componentContext: ComponentContext,
    val childFactory: (config: ScreenConfig, ctx: ComponentContext) -> RenderComponent,
) : NavigationComponentController<NavigationScreenConfig.SwitchScreenConfigContainer>(),
    RenderComponent,
    ReadinessAwaitable,
    ReturnsToFirstScreen,
    ComponentContext by componentContext {

    val child: RenderComponent = childFactory.invoke(
        hostConfig.config,
        childContext(key = "$typeId$id")
    )

    override val activeScreenConfig: ScreenConfig
        get() = hostConfig.config

    /**
     * Transparent wrapper: forward readiness-awaiting to the wrapped [child] (a switch's active
     * instance is this container, not the leaf screen), so a caller awaiting the active component
     * reaches the real screen. No-op if the child does not report readiness.
     */
    override suspend fun awaitReady() {
        (child as? ReadinessAwaitable)?.awaitReady()
    }

    /**
     * Transparent for this too, and for the same reason [awaitReady] is: a switch's active instance
     * is this container rather than the stack inside it, so a walk that stopped here would leave a
     * screen pushed inside the open tab standing.
     */
    override fun returnToFirstScreen() {
        (child as? ReturnsToFirstScreen)?.returnToFirstScreen()
    }

    /**
     * Delegates navigation to the child if it is a [NavigationComponentController].
     * A [SwitchContainerComponentController] itself is a transparent wrapper — it has no
     * stack of its own, so the only meaningful action is to forward to the inner child.
     */
    override fun navigate(
        transformer: (List<ScreenConfig>) -> List<ScreenConfig>,
        onComplete: (List<ScreenConfig>, List<ScreenConfig>) -> Unit
    ) {
        val navigableChild = child as? NavigationComponentController<*>
        if (navigableChild != null) {
            navigableChild.navigate(transformer, onComplete)
        } else {
            // No-op: the child is a leaf screen with no navigation capability.
            // Call onComplete with unchanged state to keep the contract.
            val current: List<ScreenConfig> = listOf(hostConfig.config)
            onComplete(current, current)
        }
    }

    @Composable
    override fun Render() {
        key(typeId, id) { RenderSwitchContainerComponent() }
    }

    @Composable
    private fun RenderSwitchContainerComponent() {
        key(typeId, id) { child.Render() }
    }
}
