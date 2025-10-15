package com.jamal_aliev.decompose_navigator.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.defaultComponentContext
import com.jamal_aliev.decompose_navigator.ColorConfig
import com.jamal_aliev.decompose_navigator.ScreenConfigFactory
import com.jamal_aliev.decompose_navigator.config.extraSubClasses
import com.jamal_aliev.decompose_navigator.testContentConfig1
import com.jamal_aliev.decompose_navigator.ui.theme.DecomposeNavigatorTheme
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

class MainActivity : ComponentActivity() {
    @OptIn(InternalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ctx = defaultComponentContext()

        extraSubClasses = {
            it.subclass(
                ColorConfig::class,
                ColorConfig::class.serializer(),
            )
        }

        setContent {
            DecomposeNavigatorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val rootLineNavigationComponent = remember {
                            ScreenConfigFactory.screenConfigFactory.invoke(
//                                rootLineNavigationConfig,
                                testContentConfig1,
                                ctx
                            )
                        }
                        key(rootLineNavigationComponent.typeId, rootLineNavigationComponent.id) {
                            rootLineNavigationComponent.Render()
                        }
                    }
                }
            }
        }
    }
}

