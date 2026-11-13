package uz.dokon.hisob

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Card as M3Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/*
 * Butun ilova bo'ylab "3D" ko'rinish: bu fayldagi Card, Button va OutlinedButton shu paketdagi
 * barcha ekranlarda material3 ning oddiy versiyalari o'rniga ishlatiladi
 * (bir xil paketdagi e'lon yulduzcha-import'dan ustun turadi).
 * click3d: bosilganda kichrayadigan va xira bo'ladigan bosish effekti.
 */

fun Modifier.click3d(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.95f else 1f, label = "click3d")
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (pressed && enabled) 0.88f else 1f
        }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.White),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    M3Card(
        modifier = modifier.shadow(
            elevation = 9.dp,
            shape = shape,
            ambientColor = Color(0x44000000),
            spotColor = Color(0x66000000)
        ),
        shape = shape,
        colors = colors,
        border = BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0xFFCBD5E1)))),
        content = content
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.94f else 1f, label = "btn")
    val top = if (!enabled) Color(0xFFE2E8F0) else if (pressed) Color(0xFF0F9488) else Color(0xFF2DD4BF)
    val bottom = if (!enabled) Color(0xFFCBD5E1) else if (pressed) Color(0xFF0B5E57) else Color(0xFF0F766E)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = 50.dp)
            .shadow(
                elevation = if (!enabled) 0.dp else if (pressed) 1.dp else 10.dp,
                shape = shape,
                ambientColor = Color(0x55000000),
                spotColor = Color(0xFF0F766E)
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x11000000))), shape)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides (if (enabled) Color.White else Color(0xFF64748B))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

/** Oq "3D" tugma (ikkinchi darajali amallar uchun). */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.95f else 1f, label = "obtn")
    val top = if (pressed) Color(0xFFE2E8F0) else Color.White
    val bottom = if (pressed) Color(0xFFCBD5E1) else Color(0xFFEFF3F7)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = 48.dp)
            .shadow(
                elevation = if (!enabled) 0.dp else if (pressed) 1.dp else 6.dp,
                shape = shape,
                ambientColor = Color(0x44000000),
                spotColor = Color(0x55000000)
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, Color(if (enabled) 0xFF0F766E else 0xFFCBD5E1), shape)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides (if (enabled) Color(0xFF0F766E) else Color(0xFF94A3B8))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}
