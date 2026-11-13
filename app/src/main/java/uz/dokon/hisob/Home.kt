package uz.dokon.hisob

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HomeItem(
    val key: String,
    val title: String,
    val desc: String,
    val glyph: String,
    val c1: Color,
    val c2: Color
)

/** Bosh sahifa: katta 3D tugmalar to'ri. Har bir tugma bosilganda kichrayib, soya pasayadi. */
@Composable
fun HomeScreen(items: List<HomeItem>, badge: (String) -> Int, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        SyncNotice()
        Spacer(Modifier.height(4.dp))
        for (row in items.chunked(2)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (item in row) {
                    Tile3D(item, badge(item.key), Modifier.weight(1f)) { onOpen(item.key) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Tile3D(item: HomeItem, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "tile")
    Box(
        modifier
            .height(132.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (pressed) 2.dp else 14.dp,
                shape = shape,
                ambientColor = Color(0x55000000),
                spotColor = item.c2
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(item.c1, item.c2)))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0xAAFFFFFF), Color(0x22000000))), shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
    ) {
        // Yaltiroq qatlam (3D effekt)
        Box(
            Modifier.fillMaxWidth().height(64.dp)
                .background(Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color.Transparent)))
        )
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text(item.glyph, fontSize = 30.sp)
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                item.desc,
                color = Color(0xDDFFFFFF),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (badge > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp).clip(CircleShape)
                    .background(Color(0xFFEF4444)).padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(badge.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
