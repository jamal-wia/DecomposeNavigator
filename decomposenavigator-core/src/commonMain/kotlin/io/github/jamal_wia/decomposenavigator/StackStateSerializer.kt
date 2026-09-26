package io.github.jamal_wia.decomposenavigator

import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.logging.Logger
import io.github.jamal_wia.decomposenavigator.logging.NoopLogger
import com.arkivanov.essenty.statekeeper.SerializableContainer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal object StackStateSerializer {

    private val navigationSerializer: KSerializer<List<ScreenConfig>> =
        ListSerializer(PolymorphicSerializer(ScreenConfig::class))

    fun saveStack(
        stack: List<ScreenConfig>,
        json: Json,
        logger: Logger = NoopLogger,
    ): SerializableContainer {
        return runCatching {
            val jsonStr: String = json.encodeToString(navigationSerializer, stack)
            SerializableContainer(value = jsonStr, strategy = String.serializer())
        }.getOrElse { e ->
            logger.e(e) { "saveStack failed, saving empty container" }
            SerializableContainer(value = "", strategy = String.serializer())
        }
    }

    fun <T : ScreenConfig> restoreStack(
        container: SerializableContainer,
        json: Json,
        logger: Logger = NoopLogger,
        fallback: () -> List<T>,
    ): List<T> {
        val jsonStr: String = container.consume(strategy = String.serializer()).orEmpty()
        // A blank string is the normal "nothing was persisted" case (first launch,
        // process death with no saved state) — not an error. Fall back silently.
        if (jsonStr.isBlank()) return fallback()
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            json.decodeFromString(navigationSerializer, jsonStr) as List<T>
        }.getOrElse { e ->
            logger.e(e) { "restoreStack failed, using fallback" }
            fallback()
        }
    }

    fun encodeToString(
        stack: List<ScreenConfig>,
        json: Json,
    ): String {
        return json.encodeToString(navigationSerializer, stack)
    }

    fun <T : ScreenConfig> decodeFromString(
        jsonStr: String,
        json: Json,
        logger: Logger = NoopLogger,
        fallback: () -> List<T>,
    ): List<T> {
        // A blank string is the normal "nothing was persisted" case (first launch,
        // process death with no saved state) — not an error. Fall back silently.
        if (jsonStr.isBlank()) return fallback()
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            json.decodeFromString(navigationSerializer, jsonStr) as List<T>
        }.getOrElse { e ->
            logger.e(e) { "decodeFromString failed, using fallback" }
            fallback()
        }
    }
}
