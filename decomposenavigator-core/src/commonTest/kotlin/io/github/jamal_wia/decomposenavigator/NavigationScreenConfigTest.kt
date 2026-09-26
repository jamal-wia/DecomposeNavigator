package io.github.jamal_wia.decomposenavigator

import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Serializable
private data object IdentityTestScreen : ScreenConfig

/**
 * Characterization tests for [NavigationScreenConfig] identity. These pin the CURRENT
 * contract: equality is based on `typeId` + `id` only (payload is ignored), and `id`
 * defaults to a random value per construction. Phase 2 changes the root id strategy,
 * so these guard against accidental regressions in the identity semantics.
 */
class NavigationScreenConfigTest {

    @Test
    fun `two configs built with default random id are not equal`() {
        val a = NavigationScreenConfig.LineNavigation(initialConfigs = listOf(IdentityTestScreen))
        val b = NavigationScreenConfig.LineNavigation(initialConfigs = listOf(IdentityTestScreen))

        assertNotEquals(a, b, "distinct random ids must produce distinct identities")
    }

    @Test
    fun `same typeId and id compare equal with matching hashCode`() {
        val a = NavigationScreenConfig.LineNavigation(
            initialConfigs = listOf(IdentityTestScreen),
            id = 7L,
        )
        val b = NavigationScreenConfig.LineNavigation(
            initialConfigs = listOf(IdentityTestScreen),
            id = 7L,
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `equality ignores payload and compares only typeId plus id`() {
        val a = NavigationScreenConfig.SwitchScreenConfigContainer(config = IdentityTestScreen, id = 1L)
        val b = NavigationScreenConfig.SwitchScreenConfigContainer(config = IdentityTestScreen, id = 1L)

        assertEquals(a, b)
    }

    @Test
    fun `different subclasses with the same id are not equal`() {
        val line = NavigationScreenConfig.LineNavigation(
            initialConfigs = listOf(IdentityTestScreen),
            id = 1L,
        )
        val container = NavigationScreenConfig.SwitchScreenConfigContainer(
            config = IdentityTestScreen,
            id = 1L,
        )

        assertNotEquals<NavigationScreenConfig>(line, container)
    }

    @Test
    fun `default typeId equals the simple class name`() {
        val line = NavigationScreenConfig.LineNavigation(initialConfigs = listOf(IdentityTestScreen))
        assertEquals("LineNavigation", line.typeId)
    }

    @Test
    fun `childStack key building blocks are stable for a fixed id`() {
        val config = NavigationScreenConfig.LineNavigation(
            initialConfigs = listOf(IdentityTestScreen),
            id = 99L,
        )
        // The controller uses "$typeId$id" as the Decompose childStack key.
        assertTrue("${config.typeId}${config.id}" == "LineNavigation99")
    }
}
