package uz.dokon.hisob

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val UNITS = listOf("dona", "kg", "litr", "metr", "komplekt", "quti")

@Composable
fun ProductsScreen() {
    val tick = Store.tick.intValue
    val all = remember(tick) { Store.db.products() }
    val summary = remember(tick) { Store.db.stockSummary() }
    var query by remember { mutableStateOf("") }
    var onlyLow by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var creating by remember { mutableStateOf(false) }

    val list = remember(query, onlyLow, all) {
        filterProducts(all, query).filter { !onlyLow || it.stock <= it.minStock }
    }
    val lowCount = all.count { it.stock <= it.minStock }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            ScreenTitle("Ombor")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Mahsulot turi", summary.count.toString(), Modifier.weight(1f))
                StatCard("Tannarxda", fmtMoney(summary.costValue), Modifier.weight(1f))
                StatCard("Sotuv narxida", fmtMoney(summary.sellValue), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            TextInput(query, { query = it }, "Qidirish (nomi yoki kodi)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = onlyLow,
                    onClick = { onlyLow = !onlyLow },
                    label = { Text("Kam qolganlar ($lowCount)") }
                )
            }

            if (list.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (all.isEmpty()) "Hali mahsulot yo'q.\nPastdagi + tugmasi bilan qo'shing." else "Hech narsa topilmadi",
                        color = Muted
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(list, key = { it.id }) { p ->
                        val low = p.stock <= p.minStock
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { editing = p }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.SemiBold)
                                    val sub = listOf(p.category, p.code).filter { it.isNotBlank() }.joinToString(" • ")
                                    if (sub.isNotEmpty()) {
                                        Text(sub, style = MaterialTheme.typography.bodySmall, color = Muted)
                                    }
                                    Text(
                                        "Olish: ${fmtMoney(p.buy)}  •  Sotish: ${fmtMoney(p.sell)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Muted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        fmtQty(p.stock),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (low) Red else Green
                                    )
                                    Text(p.unit, style = MaterialTheme.typography.bodySmall, color = Muted)
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            containerColor = Teal,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "Mahsulot qo'shish") }
    }

    if (creating) {
        ProductDialog(null, onDismiss = { creating = false }, onSaved = { creating = false })
    }
    editing?.let {
        ProductDialog(it, onDismiss = { editing = null }, onSaved = { editing = null })
    }
}

@Composable
fun ProductDialog(existing: Product?, onDismiss: () -> Unit, onSaved: (Product) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var code by remember { mutableStateOf(existing?.code ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "dona") }
    var buy by remember { mutableStateOf(existing?.buy?.takeIf { it > 0 }?.toString() ?: "") }
    var sell by remember { mutableStateOf(existing?.sell?.takeIf { it > 0 }?.toString() ?: "") }
    var stock by remember { mutableStateOf(existing?.stock?.let { if (it > 0) fmtQty(it) else "" } ?: "") }
    var minStock by remember { mutableStateOf(existing?.minStock?.let { if (it > 0) fmtQty(it) else "" } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Yangi mahsulot" else "Mahsulotni tahrirlash") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Banner(error, true)
                TextInput(name, { name = it }, "Nomi *")
                Spacer(Modifier.height(6.dp))
                TextInput(code, { code = it }, "Kod / shtrix-kod (ixtiyoriy)")
                Spacer(Modifier.height(6.dp))
                TextInput(category, { category = it }, "Kategoriya (masalan: Idish, Tozalash)")
                Spacer(Modifier.height(8.dp))
                Text("Birligi", style = MaterialTheme.typography.bodySmall, color = Muted)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UNITS.forEach { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                    }
                }
                Spacer(Modifier.height(6.dp))
                MoneyField(buy, { buy = it }, "Olish narxi")
                Spacer(Modifier.height(6.dp))
                MoneyField(sell, { sell = it }, "Sotish narxi *")
                Spacer(Modifier.height(6.dp))
                QtyField(stock, { stock = it }, if (existing == null) "Boshlang'ich qoldiq" else "Hozirgi qoldiq")
                Spacer(Modifier.height(6.dp))
                QtyField(minStock, { minStock = it }, "Minimal qoldiq (ogohlantirish uchun)")
                if (existing != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Red)
                        Spacer(Modifier.width(6.dp))
                        Text("Mahsulotni o'chirish", color = Red)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = name.trim()
                val c = code.trim()
                val sellV = sell.toMoney()
                when {
                    n.isEmpty() -> error = "Nomini kiriting"
                    sellV <= 0 -> error = "Sotish narxini kiriting"
                    c.isNotEmpty() && Store.db.codeExists(c, existing?.id ?: 0L) ->
                        error = "Bu kod boshqa mahsulotda bor"
                    else -> {
                        val p = Product(
                            id = existing?.id ?: 0L,
                            name = n,
                            code = c,
                            category = category.trim(),
                            unit = unit,
                            buy = buy.toMoney(),
                            sell = sellV,
                            stock = stock.toQty(),
                            minStock = minStock.toQty()
                        )
                        val saved = if (existing == null) p.copy(id = Store.db.insertProduct(p)) else {
                            Store.db.updateProduct(p)
                            p
                        }
                        Store.bump()
                        onSaved(saved)
                    }
                }
            }) { Text("Saqlash") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish") } }
    )

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "O'chirish",
            text = "\"${existing.name}\" ro'yxatdan olib tashlansinmi? Sotuv tarixi saqlanib qoladi.",
            confirm = "O'chirish",
            onConfirm = {
                Store.db.archiveProduct(existing.id)
                Store.bump()
                confirmDelete = false
                onDismiss()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
