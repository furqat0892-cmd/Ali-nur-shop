package uz.dokon.hisob

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ Umumiy qismlar

@Composable
fun StatRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(value, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SyncNotice() {
    if (!Store.configured()) {
        Banner("Baza sozlanmagan. Sozlamalar bo'limida Firebase havolasi va do'kon kodini kiriting.", true)
    } else {
        val e = Store.syncError
        if (e != null) Banner("Ulanish xatosi: $e", true)
    }
}

@Composable
fun ShipmentBubble(s: Shipment, footer: @Composable () -> Unit = {}) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.sender.ifBlank { "Do'kon egasi" },
                    fontWeight = FontWeight.Bold,
                    color = Teal,
                    modifier = Modifier.weight(1f)
                )
                if (s.receipt == null) {
                    Text("YANGI", color = Teal, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                }
                Text(fmtDate(s.ts), style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            if (s.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(s.note, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            for (i in s.items) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(i.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${fmtQty(i.qty)} ${i.unit} × ${fmtMoney(i.price)} = ${fmtMoney(lineTotal(i.price, i.qty))} so'm",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            StatRow("Jami", fmtMoney(s.total) + " so'm")
            footer()
        }
    }
}

@Composable
fun StatusFooter(s: Shipment) {
    val r = s.receipt
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (r == null) {
            Text("Kutilmoqda: hali qabul qilinmagan", color = Muted, fontWeight = FontWeight.Medium)
        } else {
            Text(
                if (r.allOk) "Qabul qilindi: hammasi joyida" else "Qabul qilindi: kamchilik bilan",
                color = if (r.allOk) Green else Red,
                fontWeight = FontWeight.Bold
            )
            Text(fmtDate(r.ts), style = MaterialTheme.typography.bodySmall, color = Muted)
            for (i in r.items) {
                if (i.damaged > 0.0005 || i.missing > 0.0005) {
                    val parts = ArrayList<String>()
                    if (i.damaged > 0.0005) parts.add("singan " + fmtQty(i.damaged))
                    if (i.missing > 0.0005) parts.add("yo'q " + fmtQty(i.missing))
                    Text(i.name + ": " + parts.joinToString(", "), color = Red)
                }
            }
            if (r.note.isNotBlank()) Text("Izoh: " + r.note)
        }
    }
}

// ------------------------------------------------------------------ Do'kon egasi

object ShipDraft {
    val lines = mutableStateListOf<ShipItem>()
    var note by mutableStateOf("")
}

@Composable
fun OwnerSend() {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("dona") }
    var price by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            delay(4000)
            message = null
        }
    }

    val lines = ShipDraft.lines
    val total = lines.sumOf { lineTotal(it.price, it.qty) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        ScreenTitle("Tovar jo'natish")
        SyncNotice()
        Banner(message, isError)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Yangi qator", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                TextInput(name, { name = it }, "Mahsulot nomi")
                Spacer(Modifier.height(6.dp))
                QtyField(qty, { qty = it }, "Soni")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UNITS.forEach { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                    }
                }
                Spacer(Modifier.height(6.dp))
                MoneyField(price, { price = it }, "Narxi (1 ta uchun)")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val q = qty.toQty()
                        if (name.isBlank()) {
                            isError = true
                            message = "Mahsulot nomini kiriting"
                        } else if (q <= 0) {
                            isError = true
                            message = "Sonini kiriting"
                        } else {
                            lines.add(ShipItem(name.trim(), q, unit, price.toMoney()))
                            name = ""
                            qty = ""
                            price = ""
                        }
                    }
                ) { Text("+ Ro'yxatga qo'shish") }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Jo'natiladigan tovarlar (${lines.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (lines.isEmpty()) {
            Text("Hali qator qo'shilmagan", color = Muted, modifier = Modifier.padding(vertical = 8.dp))
        }
        lines.forEachIndexed { idx, l ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(l.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${fmtQty(l.qty)} ${l.unit} × ${fmtMoney(l.price)} = ${fmtMoney(lineTotal(l.price, l.qty))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                    }
                    IconButton(onClick = { lines.removeAt(idx) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "O'chirish", tint = Red)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextInput(ShipDraft.note, { ShipDraft.note = it }, "Izoh (ixtiyoriy)")
        Spacer(Modifier.height(8.dp))
        StatRow("Jami summa", fmtMoney(total) + " so'm")
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = lines.isNotEmpty() && !sending,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (!Store.configured()) {
                    isError = true
                    message = "Avval Sozlamalar bo'limida baza havolasi va do'kon kodini kiriting"
                } else {
                    sending = true
                    val now = System.currentTimeMillis()
                    val s = Shipment(
                        id = now.toString(),
                        ts = now,
                        sender = Store.myName.ifBlank { "Do'kon egasi" },
                        note = ShipDraft.note.trim(),
                        items = lines.toList(),
                        receipt = null
                    )
                    scope.launch {
                        try {
                            Cloud.put(s)
                            lines.clear()
                            ShipDraft.note = ""
                            Store.shipments = listOf(s) + Store.shipments
                            isError = false
                            message = "Jo'natildi. Qabul qiluvchi xabarni ko'radi."
                        } catch (e: Exception) {
                            isError = true
                            message = "Jo'natib bo'lmadi: ${e.message}"
                        } finally {
                            sending = false
                        }
                    }
                }
            }
        ) { Text(if (sending) "Jo'natilmoqda..." else "Jo'natish") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun OwnerHistory() {
    val list = Store.shipments
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { ScreenTitle("Jo'natmalar") }
            OutlinedButton(onClick = { Store.refreshKey.intValue = Store.refreshKey.intValue + 1 }) { Text("Yangilash") }
        }
        SyncNotice()
        if (list.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hali jo'natma yo'q", color = Muted)
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(list, key = { it.id }) { s ->
                    ShipmentBubble(s) { StatusFooter(s) }
                }
            }
        }
    }
}

@Composable
fun OwnerDekada() {
    val ships = Store.shipments
    val periods = remember { dekadas(6) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        ScreenTitle("10 kunlik hisobot")
        SyncNotice()
        for (d in periods) {
            val sent = ships.filter { it.ts >= d.start && it.ts < d.end }
            val receipts = ships.mapNotNull { it.receipt }.filter { it.ts >= d.start && it.ts < d.end }
            val pending = sent.count { it.receipt == null }
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(d.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    StatRow("Jo'natmalar soni", sent.size.toString())
                    StatRow("Jo'natilgan summa", fmtMoney(sent.sumOf { it.total }) + " so'm")
                    StatRow("Qabul qilingan summa", fmtMoney(receipts.sumOf { it.okValue }) + " so'm", Green)
                    val loss = receipts.sumOf { it.lossValue }
                    if (loss > 0) StatRow("Singan / yo'q tovarlar", fmtMoney(loss) + " so'm", Red)
                    if (pending > 0) StatRow("Kutilayotgan jo'natmalar", pending.toString())
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ------------------------------------------------------------------ Qabul qiluvchi

@Composable
fun InboxScreen() {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val list = Store.shipments
    val cur = list.firstOrNull { it.id == selectedId }
    if (cur != null && cur.receipt == null) {
        ReceiveScreen(cur, onBack = { selectedId = null })
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ScreenTitle("Xabarlar") }
                OutlinedButton(onClick = { Store.refreshKey.intValue = Store.refreshKey.intValue + 1 }) { Text("Yangilash") }
            }
            SyncNotice()
            if (list.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Hali xabar yo'q.\nDo'kon egasi tovar jo'natganda shu yerda ko'rinadi.", color = Muted)
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(list, key = { it.id }) { s ->
                        ShipmentBubble(s) {
                            if (s.receipt == null) {
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { selectedId = s.id }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Qabul qilish")
                                }
                            } else {
                                StatusFooter(s)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Qabul qilingan (butun) tovarlarni omborga kirim qiladi. */
fun applyReceiptToStock(s: Shipment) {
    val r = s.receipt ?: return
    for ((idx, si) in s.items.withIndex()) {
        val ri = r.items.getOrNull(idx) ?: continue
        if (ri.ok <= 0.0005) continue
        val existing = Store.db.products().firstOrNull { it.name.trim().equals(si.name.trim(), ignoreCase = true) }
        if (existing != null) {
            Store.db.stockIn(existing.id, ri.ok, si.price, s.sender, existing.sell)
        } else {
            val id = Store.db.insertProduct(
                Product(name = si.name.trim(), unit = si.unit, buy = si.price, sell = si.price, stock = 0.0)
            )
            Store.db.stockIn(id, ri.ok, si.price, s.sender, si.price)
        }
    }
}

@Composable
fun ReceiveScreen(s: Shipment, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    val dmg = remember(s.id) { mutableStateListOf(*Array(s.items.size) { "" }) }
    val mis = remember(s.id) { mutableStateListOf(*Array(s.items.size) { "" }) }
    var note by remember(s.id) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val rows = s.items.mapIndexed { i, si ->
        val d = dmg[i].toQty()
        val m = mis[i].toQty()
        ReceiptItem(si.name, si.qty, si.qty - d - m, d, m, si.price)
    }
    val invalid = rows.any { it.ok < -0.0005 }
    val allOk = rows.all { it.damaged <= 0.0005 && it.missing <= 0.0005 }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Orqaga") }
            Text("Qabul qilish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            s.sender.ifBlank { "Do'kon egasi" } + " • " + fmtDate(s.ts),
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Har bir tovarni tekshiring. Hammasi to'liq va butun bo'lsa, maydonlarni bo'sh qoldiring. Singan yoki yetmagan bo'lsa, sonini yozing.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        Banner(error, true)

        s.items.forEachIndexed { i, si ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(si.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Jo'natilgan: ${fmtQty(si.qty)} ${si.unit} × ${fmtMoney(si.price)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QtyField(dmg[i], { dmg[i] = it }, "Singan", Modifier.weight(1f))
                        QtyField(mis[i], { mis[i] = it }, "Yo'q (yetmadi)", Modifier.weight(1f))
                    }
                    if (rows[i].ok < -0.0005) {
                        Text(
                            "Singan va yo'q sonining yig'indisi jo'natilgandan oshib ketdi",
                            color = Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            "Qabul qilinadi: ${fmtQty(rows[i].ok)} ${si.unit}",
                            color = Green,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextInput(note, { note = it }, "Izoh egasiga (ixtiyoriy)")
        Spacer(Modifier.height(8.dp))
        Text(
            if (allOk) "Xulosa: hammasi joyida" else "Xulosa: kamchilik bor. Egasiga qabul qildim bilan birga yuboriladi.",
            color = if (allOk) Green else Red,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Button(
            enabled = !invalid && !sending,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                sending = true
                error = null
                val receipt = Receipt(System.currentTimeMillis(), note.trim(), rows)
                val updated = s.copy(receipt = receipt)
                scope.launch {
                    try {
                        Cloud.put(updated)
                        applyReceiptToStock(updated)
                        Store.shipments = Store.shipments.map { if (it.id == updated.id) updated else it }
                        Store.bump()
                        onBack()
                    } catch (e: Exception) {
                        error = "Yuborib bo'lmadi: ${e.message}"
                        sending = false
                    }
                }
            }
        ) {
            Text(
                if (sending) "Yuborilmoqda..."
                else if (allOk) "Qabul qildim"
                else "Qabul qildim + kamchiliklarni yuborish"
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
