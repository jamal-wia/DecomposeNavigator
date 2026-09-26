package io.github.jamal_wia.decomposenavigator.state

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Serializable
private data class SavedThing(val text: String = "", val count: Int = 0)

/** Decodes by throwing — simulates persisted bytes that can no longer be parsed after a schema change. */
private object ThrowingDeserializer : KSerializer<SavedThing> {
    override val descriptor: SerialDescriptor = SavedThing.serializer().descriptor
    override fun serialize(encoder: Encoder, value: SavedThing) =
        SavedThing.serializer().serialize(encoder, value)

    override fun deserialize(decoder: Decoder): SavedThing =
        throw SerializationException("simulated incompatible saved state")
}

/**
 * Verifies the process-death contract of [persistedSlice]. Each test saves the keeper and then
 * round-trips it through [SerializableContainer] serialization ([reload]) — the same thing the
 * Android saved-state Bundle does on process death — so the assertions exercise real serialization,
 * not an in-memory shortcut. A failed/absent restore must return `null`, and the `snapshot` lambda
 * must always read the CURRENT source of truth rather than a value frozen at registration time —
 * that is the whole point of the lazy-supplier design (no second value to keep in sync).
 */
class PersistedSliceTest {

    private val json: Json = Json

    private fun contextWith(keeper: StateKeeperDispatcher): DefaultComponentContext =
        DefaultComponentContext(lifecycle = LifecycleRegistry(), stateKeeper = keeper)

    /** Serializes the saved state and rebuilds a dispatcher from it — mimics a real process kill. */
    private fun StateKeeperDispatcher.reload(): StateKeeperDispatcher {
        val encoded: String = json.encodeToString(SerializableContainer.serializer(), save())
        return StateKeeperDispatcher(json.decodeFromString(SerializableContainer.serializer(), encoded))
    }

    @Test
    fun `restores the saved value across a save-restore cycle`() {
        val keeper1 = StateKeeperDispatcher()
        contextWith(keeper1).persistedSlice("thing", SavedThing.serializer()) {
            SavedThing(text = "hello", count = 7)
        }

        val restored: SavedThing? = contextWith(keeper1.reload())
            .persistedSlice("thing", SavedThing.serializer()) { SavedThing() }

        assertEquals(SavedThing(text = "hello", count = 7), restored)
    }

    @Test
    fun `returns null when nothing was saved`() {
        val restored: SavedThing? = contextWith(StateKeeperDispatcher())
            .persistedSlice("thing", SavedThing.serializer()) { SavedThing() }

        assertNull(restored)
    }

    @Test
    fun `snapshot re-reads the live source of truth rather than a value frozen at registration time`() {
        // Mirrors a screen whose own state mutates after construction: the caller doesn't write to
        // any second "saved" value — the lambda simply re-reads the caller's live variable.
        var liveState = SavedThing(text = "first", count = 1)
        val keeper1 = StateKeeperDispatcher()
        contextWith(keeper1).persistedSlice("thing", SavedThing.serializer()) { liveState }
        liveState = SavedThing(text = "second", count = 2)

        val restored: SavedThing? = contextWith(keeper1.reload())
            .persistedSlice("thing", SavedThing.serializer()) { SavedThing() }

        assertEquals(SavedThing(text = "second", count = 2), restored)
    }

    @Test
    fun `returns null when restoring the saved bytes throws — falling back like nothing was saved`() {
        // Simulates a slice-schema change across an app update: the persisted bytes can no longer be
        // decoded into the current type. The caller must fall back to its own default rather than
        // crash the screen on cold start (mirrors the runCatching fallback in StackStateSerializer).
        val keeper1 = StateKeeperDispatcher()
        contextWith(keeper1).persistedSlice("thing", SavedThing.serializer()) {
            SavedThing(text = "stale", count = 5)
        }

        val restored: SavedThing? = contextWith(keeper1.reload())
            .persistedSlice("thing", ThrowingDeserializer) { SavedThing() }

        assertNull(restored)
    }

    @Test
    fun `two keys in the same component are independent`() {
        val keeper1 = StateKeeperDispatcher()
        val ctx1: DefaultComponentContext = contextWith(keeper1)
        ctx1.persistedSlice("a", SavedThing.serializer()) { SavedThing(text = "A", count = 1) }
        ctx1.persistedSlice("b", SavedThing.serializer()) { SavedThing(text = "B", count = 2) }

        val ctx2: DefaultComponentContext = contextWith(keeper1.reload())
        val a: SavedThing? = ctx2.persistedSlice("a", SavedThing.serializer()) { SavedThing() }
        val b: SavedThing? = ctx2.persistedSlice("b", SavedThing.serializer()) { SavedThing() }

        assertEquals(SavedThing(text = "A", count = 1), a)
        assertEquals(SavedThing(text = "B", count = 2), b)
    }
}
