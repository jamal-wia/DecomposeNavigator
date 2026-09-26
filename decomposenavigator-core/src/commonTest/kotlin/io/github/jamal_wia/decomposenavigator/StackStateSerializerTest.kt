package io.github.jamal_wia.decomposenavigator

import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import com.arkivanov.essenty.statekeeper.SerializableContainer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
internal data object SerializerTestScreenA : ScreenConfig

@Serializable
internal data class SerializerTestScreenB(val number: Int) : ScreenConfig

/**
 * Characterization tests for [StackStateSerializer]: they pin the CURRENT
 * save/restore/encode/decode behavior (including the fallback-on-failure contract)
 * before any refactoring touches serialization.
 */
class StackStateSerializerTest {

    private val json: Json = ScreenConfigRegistry {
        registerSerializer(SerializerTestScreenA.serializer())
        registerSerializer(SerializerTestScreenB.serializer())
    }.json

    @Test
    fun `saveStack then restoreStack round-trips the stack`() {
        val original: List<ScreenConfig> = listOf(SerializerTestScreenA, SerializerTestScreenB(5))

        val container: SerializableContainer = StackStateSerializer.saveStack(original, json)
        val restored: List<ScreenConfig> =
            StackStateSerializer.restoreStack(container, json) { error("fallback must not be used") }

        assertEquals(original, restored)
    }

    @Test
    fun `encodeToString then decodeFromString round-trips the stack`() {
        val original: List<ScreenConfig> = listOf(SerializerTestScreenB(2), SerializerTestScreenA)

        val encoded: String = StackStateSerializer.encodeToString(original, json)
        val decoded: List<ScreenConfig> =
            StackStateSerializer.decodeFromString(encoded, json) { error("fallback must not be used") }

        assertEquals(original, decoded)
    }

    @Test
    fun `decodeFromString returns fallback on malformed json`() {
        val fallback: List<ScreenConfig> = listOf(SerializerTestScreenA)

        val decoded: List<ScreenConfig> =
            StackStateSerializer.decodeFromString("{ not valid json", json) { fallback }

        assertEquals(fallback, decoded)
    }

    @Test
    fun `decodeFromString returns fallback on empty string`() {
        val fallback: List<ScreenConfig> = listOf(SerializerTestScreenB(42))

        val decoded: List<ScreenConfig> =
            StackStateSerializer.decodeFromString("", json) { fallback }

        assertEquals(fallback, decoded)
    }

    @Test
    fun `decodeFromString returns fallback on blank string`() {
        val fallback: List<ScreenConfig> = listOf(SerializerTestScreenB(7))

        val decoded: List<ScreenConfig> =
            StackStateSerializer.decodeFromString("   \n\t ", json) { fallback }

        assertEquals(fallback, decoded)
    }

    @Test
    fun `restoreStack returns fallback when container holds an empty string`() {
        // Mirrors first-launch / no-persisted-state: saveStack writes "" on failure,
        // and a fresh stateKeeper yields a blank string. Must fall back, not crash.
        val emptyContainer = SerializableContainer(
            value = "",
            strategy = String.serializer(),
        )
        val fallback: List<ScreenConfig> = listOf(SerializerTestScreenA)

        val restored: List<ScreenConfig> =
            StackStateSerializer.restoreStack(emptyContainer, json) { fallback }

        assertEquals(fallback, restored)
    }

    @Test
    fun `NavigationScreenConfig containers survive a round-trip with their id`() {
        val container = NavigationScreenConfig.SwitchScreenConfigContainer(
            config = SerializerTestScreenB(7),
            id = 123L,
        )
        val original: List<ScreenConfig> = listOf(container)

        val encoded: String = StackStateSerializer.encodeToString(original, json)
        val decoded: List<ScreenConfig> =
            StackStateSerializer.decodeFromString(encoded, json) { error("fallback must not be used") }

        val decodedContainer = decoded.single() as NavigationScreenConfig.SwitchScreenConfigContainer
        assertEquals(123L, decodedContainer.id)
        assertEquals(SerializerTestScreenB(7), decodedContainer.config)
    }

    @Test
    fun `navigation config id is always written encodeDefaults makes restore deterministic`() {
        val container = NavigationScreenConfig.SwitchScreenConfigContainer(
            config = SerializerTestScreenA,
            id = 555L,
        )

        val encoded: String = StackStateSerializer.encodeToString(listOf(container), json)

        assertTrue(encoded.contains("555"), "expected id 555 in serialized output: $encoded")
    }

    @Test
    fun `type discriminator field is named type`() {
        val encoded: String = StackStateSerializer.encodeToString(listOf(SerializerTestScreenA), json)
        assertTrue(encoded.contains("\"type\""), "expected discriminator field \"type\" in: $encoded")
    }
}
