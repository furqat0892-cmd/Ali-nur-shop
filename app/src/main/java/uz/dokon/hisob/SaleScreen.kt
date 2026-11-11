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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Savat tab almashganda yo'qolmasligi uchun alohida saqlanadi. */
object SaleDraft {
    val cart = mutableStateListOf<CartLine>()
    var method by mutableStateOf("Naqd")
    var customer by mutableStateOf<Customer?>(null)
    var paid by mutableStateOf("")

    fun add(p: Product) {
        val i = cart.indexOfFirst { it.product.id == p.id }
        if (i >= 0) {
            cart[i] = cart[i].copy(qty = cart[i].qty + 1)
        } else {
            cart.add(CartLine(p, 1.0, p.sell))
        }
    }

    fun clear() {
        cart.clear()
        method = "Naqd"
        customer = null
        paid = ""
    }
}

@Composable
fun SaleScreen() {
    val tick = Store.tick.intValue
    val products = remember(tick) { Store.db.products() }
    val cart = SaleDraft.cart
    var query by remember { mutableStateOf("") }
    var pickCustomer by remember { mutableStateOf(false) }
    var editIndex by remember { mutableIntStateOf(-1) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            delay(3500)
            message = null
        }
    }

    val total = cart.sumOf { lineTotal(it.price, it.qty) }
    val matches = remember(query, products) {
        if (query.isBlank()) emptyList() else filterProducts(products, query).take(6)
    }
    val method = SaleDraft.method

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        ScreenTitle("Sotuv")
        Banner(message, isError)
        TextInput(query, { query = it }, "Mahsulot qidirish (nomi yoki kodi)")

        if (matches.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Column {
                    matches.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                SaleDraft.add(p)
                                query = ""
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "Omborda: ${fmtQty(p.stock)} ${p.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (p.stock <= 0) Red else Muted
                                )
                            }
                            Text(fmtMoney(p.sell), fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                    }
                }
            }
        } else if (query.isNotBlank()) {
            Text("Mahsulot topilmadi", color = Muted, modifier = Modifier.padding(8.dp))
        }

        Spacer(Modifier.height(4.dp))

        if (cart.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (products.isEmpty()) "Avval \"Ombor\" bo'limida mahsulot qo'shing"
                    else "Savat bo'sh. Yuqoridan mahsulot qidiring.",
                    color = Muted
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(cart) { i, line ->
                    CartRow(
                        line = line,
                        onMinus = {
                            if (line.qty > 1) cart[i] = line.copy(qty = line.qty - 1) else cart.removeAt(i)
                        },
                        onPlus = { cart[i] = line.copy(qty = line.qty + 1) },
                        onEdit = { editIndex = i },
                        onRemove = { cart.removeAt(i) }
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Naqd", "Karta", "Nasiya").forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { SaleDraft.method = m },
                            label = { Text(m) }
                        )
                    }
                }
                if (method == "Nasiya") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { pickCustomer = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(SaleDraft.customer?.name ?: "Mijozni tanlang")
                    }
                    Spacer(Modifier.height(4.dp))
                    MoneyField(SaleDraft.paid, { SaleDraft.paid = it }, "Hozir to'langan summa (ixtiyoriy)")
                    val paidNow = minOf(SaleDraft.paid.toMoney(), total)
                    Text(
                        "Qarzga qoladi: ${fmtMoney(total - paidNow)} so'm",
                        color = Red,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Jami", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(
                            fmtMoney(total) + " so'm",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        enabled = cart.isNotEmpty(),
                        onClick = {
                            val customer = SaleDraft.customer
                            if (method == "Nasiya" && customer == null) {
                                isError = true
                                message = "Nasiya uchun mijozni tanlang"
                            } else {
                                val paid = if (method == "Nasiya") minOf(SaleDraft.paid.toMoney(), total) else total
                                Store.db.createSale(cart.toList(), method, customer?.id, paid)
                                SaleDraft.clear()
                                Store.bump()
                                isError = false
                                message = "Sotuv saqlandi: ${fmtMoney(total)} so'm"
                            }
                        }
                    ) { Text("Sotish", fontSize = 18.sp) }
                }
            }
        }
    }

    if (pickCustomer) {
        CustomerPicker(
            onDismiss = { pickCustomer = false },
            onPick = {
                SaleDraft.customer = it
                pickCustomer = false
            }
        )
    }

    if (editIndex in cart.indices) {
        val line = cart[editIndex]
        var qtyT by remember(editIndex) { mutableStateOf(fmtQty(line.qty)) }
        var priceT by remember(editIndex) { mutableStateOf(line.price.toString()) }
        AlertDialog(
            onDismissRequest = { editIndex = -1 },
            title = { Text(line.product.name) },
            text = {
                Column {
                    QtyField(qtyT, { qtyT = it }, "Miqdor (${line.product.unit})")
                    Spacer(Modifier.height(8.dp))
                    MoneyField(priceT, { priceT = it }, "Narxi (1 ${line.product.unit})")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val q = qtyT.toQty()
                    if (q > 0 && editIndex in cart.indices) {
                        cart[editIndex] = line.copy(qty = q, price = priceT.toMoney())
                    }
                    editIndex = -1
                }) { Text("Saqlash") }
            },
            dismissButton = { TextButton(onClick = { editIndex = -1 }) { Text("Bekor qilish") } }
        )
    }
}

@Composable
private fun CartRow(
    line: CartLine,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(line.product.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "O'chirish", tint = Red)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                OutlinedButton(
                    onClick = onMinus,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp)
                ) { Text("−", fontSize = 20.sp) }
                Text(
                    fmtQty(line.qty) + " " + line.product.unit,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onEdit).padding(horizontal = 12.dp, vertical = 8.dp)
                )
                OutlinedButton(
                    onClick = onPlus,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp)
                ) { Text("+", fontSize = 20.sp) }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.clickable(onClick = onEdit)) {
                    Text(fmtMoney(lineTotal(line.price, line.qty)), fontWeight = FontWeight.Bold)
                    Text(
                        "${fmtMoney(line.price)} × ${fmtQty(line.qty)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }
            if (line.qty > line.product.stock) {
                Text(
                    "Omborda faqat ${fmtQty(line.product.stock)} ${line.product.unit} bor",
                    color = Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
