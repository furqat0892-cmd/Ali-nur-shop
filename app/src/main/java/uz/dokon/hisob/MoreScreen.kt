package uz.dokon.hisob

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MoreScreen(start: Int = -1) {
    if (start >= 0) {
        // Bosh sahifadan to'g'ridan-to'g'ri ochilgan bo'lim (orqaga tugmasi yuqori panelda)
        when (start) {
            0 -> SalesHistory()
            1 -> Customers()
            2 -> BackupExport()
            else -> SettingsScreen(firstRun = false)
        }
        return
    }
    var section by remember { mutableIntStateOf(-1) }
    when (section) {
        0 -> Sub("Sotuvlar tarixi", { section = -1 }) { SalesHistory() }
        1 -> Sub("Mijozlar va qarzlar", { section = -1 }) { Customers() }
        2 -> Sub("Zaxira va eksport", { section = -1 }) { BackupExport() }
        3 -> Sub("Sozlamalar", { section = -1 }) { SettingsScreen(firstRun = false) }
        else -> Column(Modifier.fillMaxSize().padding(12.dp)) {
            ScreenTitle("Yana")
            listOf(
                "Sotuvlar tarixi" to "Barcha cheklar, bekor qilish",
                "Mijozlar va qarzlar" to "Nasiya savdo, qarz to'lovlari",
                "Zaxira va eksport" to "Ma'lumotni saqlash, Excel (CSV)",
                "Sozlamalar" to "Ism, baza havolasi, do'kon kodi, chiqish"
            ).forEachIndexed { i, (t, d) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).click3d { section = i }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(t, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(d, color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Sub(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Orqaga") }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun SalesHistory() {
    val tick = Store.tick.intValue
    val list = remember(tick) { Store.db.sales(200) }
    var cancelId by remember { mutableStateOf<Long?>(null) }

    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Hali sotuv yo'q", color = Muted) }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        items(list, key = { it.id }) { s ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Chek #${s.id}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            fmtMoney(s.total),
                            fontWeight = FontWeight.Bold,
                            color = if (s.cancelled) Muted else androidx.compose.ui.graphics.Color.Unspecified
                        )
                    }
                    Text(
                        fmtDate(s.ts) + " • " + s.method + (if (s.customer.isNotBlank()) " • ${s.customer}" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Text(s.items, style = MaterialTheme.typography.bodyMedium)
                    if (s.total > s.paid && !s.cancelled) {
                        Text("Qarz: ${fmtMoney(s.total - s.paid)}", color = Red, style = MaterialTheme.typography.bodySmall)
                    }
                    if (s.cancelled) {
                        Text("BEKOR QILINGAN", color = Red, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    } else {
                        TextButton(onClick = { cancelId = s.id }) { Text("Sotuvni bekor qilish", color = Red) }
                    }
                }
            }
        }
    }
    cancelId?.let { id ->
        ConfirmDialog(
            title = "Sotuvni bekor qilish",
            text = "Mahsulotlar omborga qaytariladi va qarz (bo'lsa) o'chiriladi.",
            confirm = "Bekor qilish",
            onConfirm = {
                Store.db.cancelSale(id)
                Store.bump()
                cancelId = null
            },
            onDismiss = { cancelId = null }
        )
    }
}

@Composable
private fun Customers() {
    val tick = Store.tick.intValue
    val list = remember(tick) { Store.db.customers() }
    var adding by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<Customer?>(null) }
    val totalDebt = list.sumOf { maxOf(it.debt, 0L) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        StatCard("Jami qarzdorlik", fmtMoney(totalDebt) + " so'm", Modifier.fillMaxWidth(), if (totalDebt > 0) Red else androidx.compose.ui.graphics.Color.Unspecified)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Yangi mijoz") }
        Spacer(Modifier.height(4.dp))
        if (list.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hali mijoz yo'q", color = Muted)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(list, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.SemiBold)
                                if (c.phone.isNotBlank()) Text(c.phone, style = MaterialTheme.typography.bodySmall, color = Muted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (c.debt > 0) fmtMoney(c.debt) else "Qarz yo'q",
                                    fontWeight = FontWeight.Bold,
                                    color = if (c.debt > 0) Red else Green
                                )
                                if (c.debt > 0) TextButton(onClick = { paying = c }) { Text("To'lov olish") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Yangi mijoz") },
            text = {
                Column {
                    TextInput(name, { name = it }, "Ismi *")
                    Spacer(Modifier.height(6.dp))
                    TextInput(phone, { phone = it }, "Telefon (ixtiyoriy)")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        Store.db.addCustomer(name.trim(), phone.trim())
                        Store.bump()
                        adding = false
                    }
                }) { Text("Saqlash") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Bekor qilish") } }
        )
    }

    paying?.let { c ->
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { paying = null },
            title = { Text(c.name) },
            text = {
                Column {
                    Text("Qarz: ${fmtMoney(c.debt)} so'm", color = Red)
                    Spacer(Modifier.height(8.dp))
                    MoneyField(amount, { amount = it }, "To'langan summa")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val a = minOf(amount.toMoney(), c.debt)
                    if (a > 0) {
                        Store.db.receivePayment(c.id, a)
                        Store.bump()
                        paying = null
                    }
                }) { Text("Qabul qilish") }
            },
            dismissButton = { TextButton(onClick = { paying = null }) { Text("Bekor qilish") } }
        )
    }
}

@Composable
private fun BackupExport() {
    val ctx = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            delay(5000)
            message = null
        }
    }

    fun write(uri: android.net.Uri?, bytes: ByteArray, ok: String) {
        if (uri == null) return
        try {
            ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(bytes) }
            isError = false
            message = ok
        } catch (e: Exception) {
            isError = true
            message = "Saqlab bo'lmadi: ${e.message}"
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        Store.db.writableDatabase.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
        write(uri, Store.db.dbFile().readBytes(), "Zaxira nusxa saqlandi")
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                pendingRestore = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            } catch (e: Exception) {
                isError = true
                message = "Faylni o'qib bo'lmadi"
            }
        }
    }
    val productsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        write(uri, Store.db.productsCsv().toByteArray(Charsets.UTF_8), "Mahsulotlar ro'yxati saqlandi")
    }
    val salesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        write(uri, Store.db.salesCsv().toByteArray(Charsets.UTF_8), "Sotuvlar ro'yxati saqlandi")
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Banner(message, isError)
        Text("Zaxira nusxa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Telefon yo'qolsa yoki almashtirsangiz, ma'lumotlar yo'qolmasligi uchun zaxira nusxani vaqti-vaqti bilan saqlab turing (Google Drive yoki telefon xotirasiga).",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        Button(onClick = { backupLauncher.launch("dokon-zaxira-${fileDate()}.db") }, modifier = Modifier.fillMaxWidth()) {
            Text("Zaxira nusxa saqlash")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Zaxiradan tiklash")
        }
        Spacer(Modifier.height(24.dp))
        Text("Excel uchun eksport (CSV)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { productsLauncher.launch("mahsulotlar-${fileDate()}.csv") }, modifier = Modifier.fillMaxWidth()) {
            Text("Mahsulotlar ro'yxati")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { salesLauncher.launch("sotuvlar-${fileDate()}.csv") }, modifier = Modifier.fillMaxWidth()) {
            Text("Barcha sotuvlar")
        }
    }

    pendingRestore?.let { bytes ->
        ConfirmDialog(
            title = "Zaxiradan tiklash",
            text = "Hozirgi barcha ma'lumotlar o'chirilib, zaxiradagi ma'lumotlar bilan almashtiriladi. Davom etasizmi?",
            confirm = "Tiklash",
            onConfirm = {
                val ok = Store.restore(bytes)
                pendingRestore = null
                isError = !ok
                message = if (ok) "Ma'lumotlar tiklandi" else "Bu fayl zaxira nusxa emas yoki buzilgan"
            },
            onDismiss = { pendingRestore = null }
        )
    }
}
