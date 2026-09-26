package io.github.jamal_wia.decomposenavigator

import io.github.jamal_wia.decomposenavigator.config.NavigationScreenConfig
import io.github.jamal_wia.decomposenavigator.config.ScreenConfig
import io.github.jamal_wia.decomposenavigator.controller.LineNavigationComponentController
import io.github.jamal_wia.decomposenavigator.controller.SwitchContainerComponentController
import io.github.jamal_wia.decomposenavigator.controller.SwitchNavigationComponentController
import io.github.jamal_wia.decomposenavigator.controller.TabNavigationComponentController
import io.github.jamal_wia.decomposenavigator.logging.Logger
import io.github.jamal_wia.decomposenavigator.logging.NoopLogger
import com.arkivanov.decompose.ComponentContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

typealias ScreenConfigComponentFactory = (config: ScreenConfig, ctx: ComponentContext) -> RenderComponent

typealias NavigationComponentFactory = (
    config: NavigationScreenConfig,
    ctx: ComponentContext,
    childFactory: ScreenConfigComponentFactory,
    json: Json,
) -> RenderComponent

class ScreenConfigRegistry private constructor(
    private val factories: Map<KClass<out ScreenConfig>, ScreenConfigComponentFactory>,
    private val navigationFactories: Map<KClass<out NavigationScreenConfig>, NavigationComponentFactory>,
    serializers: List<SerializerRegistration<out ScreenConfig>>,
    additionalModules: List<SerializersModule>,
    /** Passed to every built-in controller this registry creates. See [Builder.logger]. */
    private val logger: Logger,
) {

    val screenConfigPolymorphicSerializerModule = SerializersModule {
        polymorphic(ScreenConfig::class) {
            subclass(
                NavigationScreenConfig.LineNavigation::class,
                NavigationScreenConfig.LineNavigation.serializer()
            )
            subclass(
                NavigationScreenConfig.SwitchScreen::class,
                NavigationScreenConfig.SwitchScreen.serializer()
            )
            subclass(
                NavigationScreenConfig.SwitchScreenConfigContainer::class,
                NavigationScreenConfig.SwitchScreenConfigContainer.serializer()
            )
            subclass(
                NavigationScreenConfig.TabNavigation::class,
                NavigationScreenConfig.TabNavigation.serializer()
            )
            serializers.forEach { serializer: SerializerRegistration<out ScreenConfig> ->
                serializer.register(this)
            }
        }
        additionalModules.forEach { include(it) }
    }

    val json = Json {
        serializersModule = this@ScreenConfigRegistry.screenConfigPolymorphicSerializerModule
        classDiscriminator = "type"
        // Force-encode properties that have defaults — needed because of NavigationScreenConfig.id,
        // which is part of the childStack restore key ("$typeId$id") and defaults to randomId().
        // With encodeDefaults=false, kotlinx decides whether to write a defaulted property by
        // comparing the value against a *freshly re-evaluated* default expression — for a random
        // default that means comparing against a new random number each time. In practice id is
        // still written (two randoms almost never match), so this is NOT what was dropping saved
        // state — the lost-root-stack bug was a random root id, fixed separately. But we must not
        // let the presence of a restore key in the saved JSON hinge on that value-dependent
        // heuristic. encodeDefaults=true makes encoding unconditional: id (and typeId) are always
        // serialized, so save/restore is deterministic by construction rather than by chance.
        encodeDefaults = true
    }

    fun createComponent(config: ScreenConfig, ctx: ComponentContext): RenderComponent {
        return when (config) {
            is NavigationScreenConfig -> createNavigationComponent(config, ctx)
            else -> createCustomComponent(config, ctx)
        }
    }

    private fun createNavigationComponent(
        config: NavigationScreenConfig,
        ctx: ComponentContext
    ): RenderComponent {
        val customFactory: NavigationComponentFactory? = navigationFactories[config::class]
        if (customFactory != null) {
            return customFactory(config, ctx, ::createComponent, json)
        }

        return when (config) {
            is NavigationScreenConfig.LineNavigation -> {
                LineNavigationComponentController(
                    hostConfig = config,
                    componentContext = ctx,
                    childFactory = ::createComponent,
                    json = json,
                    logger = logger
                )
            }

            is NavigationScreenConfig.SwitchScreen -> {
                SwitchNavigationComponentController(
                    hostConfig = config,
                    componentContext = ctx,
                    childFactory = ::createComponent,
                    json = json,
                    logger = logger
                )
            }

            is NavigationScreenConfig.SwitchScreenConfigContainer -> {
                SwitchContainerComponentController(
                    hostConfig = config,
                    componentContext = ctx,
                    childFactory = ::createComponent
                )
            }

            is NavigationScreenConfig.TabNavigation -> {
                TabNavigationComponentController(
                    hostConfig = config,
                    componentContext = ctx,
                    childFactory = ::createComponent
                )
            }

            else -> throw IllegalArgumentException(
                "No navigation controller registered for config: ${config::class.simpleName}. " +
                        "Use registerNavigationController<${config::class.simpleName}>() " +
                        "in ScreenConfigRegistry builder."
            )
        }
    }

    private fun createCustomComponent(
        config: ScreenConfig,
        ctx: ComponentContext
    ): RenderComponent {
        val factory: ScreenConfigComponentFactory = factories[config::class]
            ?: throw IllegalArgumentException(
                "No factory registered for config: ${config::class.simpleName}. " +
                        "Did you forget to register it in ScreenConfigRegistry?"
            )
        return factory(config, ctx)
    }

    class Builder @PublishedApi internal constructor() {

        /**
         * Diagnostic sink passed to every built-in controller this registry creates. Defaults to
         * [NoopLogger] (silent); set it once to wire an app-supplied [Logger] — no DI framework is
         * imposed, this is a plain property on the builder.
         */
        var logger: Logger = NoopLogger

        @PublishedApi
        internal val factories =
            mutableMapOf<KClass<out ScreenConfig>, ScreenConfigComponentFactory>()

        @PublishedApi
        internal val navigationFactories =
            mutableMapOf<KClass<out NavigationScreenConfig>, NavigationComponentFactory>()

        @PublishedApi
        internal val serializerRegistrations =
            mutableListOf<SerializerRegistration<out ScreenConfig>>()

        @PublishedApi
        internal val additionalModules =
            mutableListOf<SerializersModule>()

        /**
         * Register an additional [SerializersModule] to be included in the Json instance.
         * Use this to add polymorphic serializers for non-ScreenConfig types.
         */
        fun registerModule(module: SerializersModule) {
            additionalModules.add(module)
        }

        /**
         * Register a serializer for a specific ScreenConfig type.
         */
        inline fun <reified T : ScreenConfig> registerSerializer(serializer: KSerializer<T>) {
            serializerRegistrations.add(SerializerRegistration(T::class, serializer))
        }

        /**
         * Register a factory for a specific ScreenConfig type.
         */
        inline fun <reified T : ScreenConfig> register(
            noinline factory: (config: T, ctx: ComponentContext) -> RenderComponent
        ) {
            @Suppress("UNCHECKED_CAST")
            factories[T::class] = factory as ScreenConfigComponentFactory
        }

        /**
         * Register both factory and serializer for a specific ScreenConfig type.
         */
        inline fun <reified T : ScreenConfig> registerWithSerializer(
            serializer: KSerializer<T>,
            noinline factory: (config: T, ctx: ComponentContext) -> RenderComponent
        ) {
            register(factory)
            registerSerializer(serializer)
        }

        /**
         * Override the default controller for a NavigationScreenConfig subclass.
         */
        inline fun <reified T : NavigationScreenConfig> registerNavigationController(
            noinline factory: (
                config: T,
                ctx: ComponentContext,
                childFactory: ScreenConfigComponentFactory,
                json: Json,
            ) -> RenderComponent
        ) {
            @Suppress("UNCHECKED_CAST")
            navigationFactories[T::class] = factory as NavigationComponentFactory
        }

        /**
         * Override the default controller and register a serializer for a custom NavigationScreenConfig subclass.
         */
        inline fun <reified T : NavigationScreenConfig> registerNavigationControllerWithSerializer(
            serializer: KSerializer<T>,
            noinline factory: (
                config: T,
                ctx: ComponentContext,
                childFactory: ScreenConfigComponentFactory,
                json: Json,
            ) -> RenderComponent
        ) {
            registerNavigationController(factory)
            serializerRegistrations.add(SerializerRegistration(T::class, serializer))
        }

        fun build() = ScreenConfigRegistry(
            factories = factories.toMap(),
            navigationFactories = navigationFactories.toMap(),
            serializers = serializerRegistrations.toList(),
            additionalModules = additionalModules.toList(),
            logger = logger
        )
    }

    companion object {
        operator fun invoke(block: Builder.() -> Unit): ScreenConfigRegistry {
            return Builder()
                .apply(block)
                .build()
        }
    }
}

// ── DSL extensions ──────────────────────────────────────────────────

/**
 * Register a screen using a constructor reference.
 * Usage: `screen(::MyScreenComponent)`
 */
inline fun <reified T : ScreenConfig> ScreenConfigRegistry.Builder.screen(
    noinline factory: (config: T, ctx: ComponentContext) -> RenderComponent
) {
    registerWithSerializer(serializer = serializer(), factory = factory)
}

/**
 * Register a custom navigation controller.
 * Usage: `navigationController<MyNavConfig> { config, ctx, childFactory, json -> ... }`
 */
inline fun <reified T : NavigationScreenConfig> ScreenConfigRegistry.Builder.navigationController(
    noinline factory: (
        config: T,
        ctx: ComponentContext,
        childFactory: ScreenConfigComponentFactory,
        json: Json,
    ) -> RenderComponent
) {
    registerNavigationControllerWithSerializer(serializer = serializer(), factory = factory)
}

/**
 * Register additional serializer modules.
 * Usage: `serializerModule { polymorphic(...) { ... } }`
 */
inline fun ScreenConfigRegistry.Builder.serializerModule(block: SerializersModuleBuilder.() -> Unit) {
    registerModule(SerializersModule(block))
}

/**
 * Holds serializer registration info for deferred registration.
 */
class SerializerRegistration<T : ScreenConfig>(
    private val kClass: KClass<T>,
    private val serializer: KSerializer<T>
) {
    fun register(builder: PolymorphicModuleBuilder<ScreenConfig>) {
        builder.subclass(kClass, serializer)
    }
}
