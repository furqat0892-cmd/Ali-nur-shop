package uz.dokon.hisob

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun StockScreen() {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Kirim / Chiqim",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp)
        )
        TabRow(selectedTabIndex = tab, containerColor = androidx.compose.ui.graphics.Color.Transparent) {
            listOf("Kirim", "Chiqim", "Tarix").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> MoveForm(isIn = true)
            1 -> MoveForm(isIn = false)
            else -> MoveHistory()
        }
    }
}

@Composable
private fun MoveForm(isIn: Boolean) {
    var product by remember { mutableStateOf<Product?>(null) }
    var qty by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var sell by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            delay(3500)
            message = null
        }
    }

    fun choose(p: Product) {
        product = p
        price = if (p.buy > 0) p.buy.toString() else ""
        sell = if (p.sell > 0) p.sell.toString() else ""
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Banner(message, isError)
        Text(
            if (isIn) "Yetkazib beruvchidan tovar keldi" else "Sotuvdan tashqari chiqim (singan, yaroqsiz, shaxsiy ehtiyoj)",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
            Text(product?.name ?: "Mahsulotni tanlang")
        }
        product?.let {
            Text(
                "Hozir omborda: ${fmtQty(it.stock)} ${it.unit}",
                color = Muted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        QtyField(qty, { qty = it }, "Miqdor" + (product?.let { " (${it.unit})" } ?: ""))
        Spacer(Modifier.height(8.dp))
        if (isIn) {
            MoneyField(price, { price = it }, "Olish narxi (1 ta uchun)")
            Spacer(Modifier.height(8.dp))
            MoneyField(sell, { sell = it }, "Sotish narxi (1 ta uchun)")
            val b = price.toMoney()
            val s = sell.toMoney()
            if (b > 0 && s > 0) {
                val pct = (s - b) * 100.0 / b
                Text(
                    "Ustama: ${fmtMoney(s - b)} so'm (${String.format(java.util.Locale.US, "%.0f", pct)}%)",
                    color = if (s >= b) Green else Red,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            TextInput(supplier, { supplier = it }, "Yetkazib beruvchi (ixtiyoriy)")
        } else {
            TextInput(note, { note = it }, "Sababi (masalan: singan)")
        }
        Spacer(Modifier.height(16.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val p = product
                val q = qty.toQty()
                if (p == null) {
                    isError = true
                    message = "Mahsulotni tanlang"
                } else if (q <= 0) {
                    isError = true
                    message = "Miqdorni kiriting"
                } else if (!isIn && q > p.stock) {
                    isError = true
                    message = "Omborda faqat ${fmtQty(p.stock)} ${p.unit} bor"
                } else {
                    if (isIn) {
                        val b = price.toMoney()
                        val s = if (sell.toMoney() > 0) sell.toMoney() else p.sell
                        Store.db.stockIn(p.id, q, b, supplier.trim(), s)
                    } else {
                        Store.db.stockOut(p.id, q, note.trim().ifEmpty { "Chiqim" })
                    }
                    Store.bump()
                    isError = false
                    message = (if (isIn) "Kirim saqlandi: " else "Chiqim saqlandi: ") + p.name + " × " + fmtQty(q)
                    product = null
                    qty = ""
                    price = ""
                    sell = ""
                    supplier = ""
                    note = ""
                }
            }
        ) { Text(if (isIn) "Kirimni saqlash" else "Chiqimni saqlash") }
    }

    if (picking) {
        ProductPicker(
            onDismiss = { picking = false },
            onPick = {
                choose(it)
                picking = false
            },
            onNew = if (isIn) ({
                picking = false
                creating = true
            }) else null
        )
    }
    if (creating) {
        ProductDialog(
            existing = null,
            onDismiss = { creating = false },
            onSaved = {
                creating = false
                choose(it)
            }
        )
    }
}

@Composable
private fun MoveHistory() {
    val tick = Store.tick.intValue
    val list = remember(tick) { Store.db.moves(200) }
    var delId by remember { mutableStateOf<Long?>(null) }

    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hali kirim yoki chiqim yo'q", color = Muted)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        items(list, key = { it.id }) { m ->
            val isIn = m.kind == "IN"
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.productName, fontWeight = FontWeight.SemiBold)
                        Text(fmtDate(m.ts), style = MaterialTheme.typography.bodySmall, color = Muted)
                        if (m.note.isNotBlank()) {
                            Text(m.note, style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        if (m.price > 0) {
                            Text(
                                "Narxi: ${fmtMoney(m.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            (if (isIn) "+" else "−") + fmtQty(m.qty) + " " + m.unit,
                            fontWeight = FontWeight.Bold,
                            color = if (isIn) Green else Red
                        )
                        TextButton(onClick = { delId = m.id }) { Text("Bekor qilish", color = Red) }
                    }
                }
            }
        }
    }

    delId?.let { id ->
        ConfirmDialog(
            title = "Yozuvni bekor qilish",
            text = "Bu yozuv o'chiriladi va ombor qoldig'i avvalgi holatiga qaytariladi.",
            confirm = "Bekor qilish",
            onConfirm = {
                Store.db.deleteMove(id)
                Store.bump()
                delId = null
            },
            onDismiss = { delId = null }
        )
    }
}
