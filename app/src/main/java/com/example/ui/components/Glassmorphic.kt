package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun GlassBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseBgColor = if (isDark) DarkBackground else LightBackground
    val orbColor1 = if (isDark) Color(0x3D1E40AF) else Color(0x223B82F6) // Deep Blue Glow
    val orbColor2 = if (isDark) Color(0x3D6B21A8) else Color(0x1F8B5CF6) // Deep Purple Glow
    val orbColor3 = if (isDark) Color(0x220D9488) else Color(0x150D9488) // Subtle Teal Glow

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBgColor)
    ) {
        // Render stylized decorative blur orbs on a Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Orb 1: Top Left (Blue)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbColor1, Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.1f),
                    radius = size.width * 0.8f
                ),
                center = Offset(size.width * 0.1f, size.height * 0.1f),
                radius = size.width * 0.8f
            )

            // Orb 2: Bottom Right (Purple)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbColor2, Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.8f),
                    radius = size.width * 0.85f
                ),
                center = Offset(size.width * 0.9f, size.height * 0.8f),
                radius = size.width * 0.85f
            )

            // Orb 3: Center Left (Teal)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbColor3, Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.5f),
                    radius = size.width * 0.5f
                ),
                center = Offset(size.width * 0.3f, size.height * 0.5f),
                radius = size.width * 0.5f
            )
        }

        // Overlay with a very fine matte mesh to create a realistic glass texture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(Color(0x12000000), Color(0x2A000000))
                        } else {
                            listOf(Color(0x05FFFFFF), Color(0x10000000))
                        }
                    )
                )
        ) {
            content()
        }
    }
}

@Composable
fun GlassCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        glassColor,
                        glassColor.copy(alpha = glassColor.alpha * 0.4f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(width = borderWidth, color = borderColor, shape = shape)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: List<Color> = listOf(PrimaryEmerald, SecondaryTeal),
    content: @Composable RowScope.() -> Unit
) {
    val alpha = if (enabled) 1.0f else 0.4f
    val buttonShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .clip(buttonShape)
            .background(
                Brush.horizontalGradient(
                    colors = colors.map { it.copy(alpha = it.alpha * alpha) }
                )
            )
            .clickable(enabled = enabled, onClick = onClick)
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x3DFFFFFF) else Color(0x22000000),
                shape = buttonShape
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    val glassColor = if (isDark) Color(0x1F1E293B) else Color(0x33FFFFFF)
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val textColor = if (isDark) TextPrimaryDark else TextPrimaryLight

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = if (isDark) TextSecondaryDark else TextSecondaryLight) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(glassColor),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedBorderColor = PrimaryEmerald,
            unfocusedBorderColor = borderColor,
            cursorColor = PrimaryEmerald,
            focusedLabelColor = PrimaryEmerald
        ),
        shape = RoundedCornerShape(14.dp)
    )
}
