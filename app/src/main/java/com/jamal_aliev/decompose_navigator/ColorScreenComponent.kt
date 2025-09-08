package com.jamal_aliev.decompose_navigator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jamal_aliev.decompose_navigator.navigator.LocalNavigator
import com.jamal_aliev.decompose_navigator.navigator.Navigator
import com.jamal_aliev.decompose_navigator.navigator.impl.LineNavigator
import com.jamal_aliev.decompose_navigator.navigator.impl.SwitchNavigator
import kotlin.random.Random

class ColorScreenComponent(
    private val colorHex: String,
) : RenderComponent {

    private val bg = parseHex(colorHex)

    override val typeId: String = ColorScreenComponent::class.simpleName.orEmpty()
    override val id: Long = colorHex.hashCode().toLong()

    @Composable
    override fun Render() {
        key(typeId, id, colorHex) { RenderColorScreenComponent() }
    }

    @Composable
    private fun RenderColorScreenComponent() {
        val currentNavigator: Navigator? = LocalNavigator.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        (currentNavigator as? LineNavigator)?.pushNew(
                            Config.Color(randomColorHex())
                        )
                    }) { Text("push") }

                    Button(onClick = { }) { Text("pop") }

                    Button(onClick = {
                    }) { Text("replace") }
                }

                // Buttons row 2: reset / popToRoot / switch
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                    }) { Text("reset") }

                    Button(onClick = { }) { Text("popToRoot") }

                    Button(onClick = {
                        (currentNavigator?.findAncestorBy { it is SwitchNavigator }
                                as? SwitchNavigator)
                            ?.switchTo(switchContainerDecomposeNavigationConfig1)
                    }) { Text("switch") }
                }
            }

            // Hex display at bottom center, white text inside dark rounded box
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(Color(0x66000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = colorHex.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    companion object {

        private fun parseHex(hex: String): Color {
            return try {
                // support #RRGGBB or RRGGBB
                val clean = hex.removePrefix("#")
                val int = clean.toLong(16).toInt()
                Color(0xFF000000.toInt() or int)
            } catch (_: Exception) {
                Color.Gray
            }
        }

        fun randomColorHex(): String {
            val value = Random.nextInt(0x1000000) // 0..0xFFFFFF
            val hex = value.toString(16).padStart(6, '0').uppercase()
            return "#$hex"
        }
    }
}
