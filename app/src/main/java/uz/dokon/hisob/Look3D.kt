package uz.dokon.hisob

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Butun ilova bo'ylab "3D" ko'rinish: bu fayldagi Card va Button shu paketdagi
 * barcha ekranlarda material3 ning oddiy Card/Button o'rniga ishlatiladi
 * (bir xil paketdagi e'lon yulduzcha-import'dan ustun turadi).
 */

@Composable
fun Card(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.White),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    M3Card(
        modifier = modifier.shadow(
            elevation = 10.dp,
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
    val top = if (enabled) Color(0xFF2DD4BF) else Color(0xFFE2E8F0)
    val bottom = if (enabled) Color(0xFF0F766E) else Color(0xFFCBD5E1)
    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = Color(0x55000000),
                spotColor = Color(0xFF0F766E)
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x11000000))), shape)
            .clickable(enabled = enabled, onClick = onClick),
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
