package io.github.jamal_wia.decomposenavigator.config

import io.github.jamal_wia.decomposenavigator.randomId
import kotlinx.serialization.Serializable

abstract class NavigationScreenConfig : ScreenConfig {

    abstract val typeId: String
    abstract val id: Long

    override fun hashCode(): Int {
        var result: Int = typeId.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as NavigationScreenConfig
        val result: Boolean = typeId == other.typeId && id == other.id
        return result
    }

    @Serializable
    class LineNavigation(
        val initialConfigs: List<ScreenConfig>,
        override val typeId: String = LineNavigation::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : NavigationScreenConfig()

    /** [maxBackStackSize] caps how many entries [SwitchNavigationComponentController.backStack]
     * keeps, i.e. how many distinct tabs the back button can still step through — `null` (the
     * default, and every caller except the admin app's tab shell) keeps today's unbounded
     * history. This bounds back-navigation order only: it does not evict or destroy the tab's
     * mounted component in the underlying `childStack` — a switched-away tab dropped from this
     * history stays fully alive (and keeps running whatever it started) until the switch itself
     * is torn down. */
    @Serializable
    class SwitchScreen(
        val initialConfig: SwitchScreenConfigContainer,
        override val typeId: String = SwitchScreen::class.simpleName.orEmpty(),
        override val id: Long = randomId(),
        val maxBackStackSize: Int? = null,
    ) : NavigationScreenConfig()

    @Serializable
    class SwitchScreenConfigContainer(
        val config: ScreenConfig,
        override val typeId: String = SwitchScreenConfigContainer::class.simpleName.orEmpty(),
        override val id: Long = randomId()
    ) : NavigationScreenConfig()

    /** [maxBackStackSize] is forwarded to the [SwitchScreen] this builds internally — see its
     * own doc. */
    @Serializable
    class TabNavigation(
        val initialConfig: SwitchScreenConfigContainer,
        val tabs: List<TabNavigationEntry>,
        override val typeId: String = TabNavigation::class.simpleName.orEmpty(),
        override val id: Long = randomId(),
        val maxBackStackSize: Int? = null,
    ) : NavigationScreenConfig() {

        @Serializable
        data class TabNavigationEntry(
            val name: String, // TODO возможно стоит передавать id строкового ресурса
            val container: SwitchScreenConfigContainer
        )
    }
}
