package uz.dokon.hisob

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.File

val Teal = Color(0xFF0F766E)
val Red = Color(0xFFB91C1C)
val Green = Color(0xFF15803D)
val Muted = Color(0xFF64748B)

/** Butun ilova bo'ylab umumiy holat: baza va yangilanish hisoblagichi. */
object Store {
    lateinit var app: Context
    lateinit var db: Db
    val tick = mutableIntStateOf(0)

    // Rol va bulut (Firebase) sozlamalari
    lateinit var prefs: android.content.SharedPreferences
    var role by mutableStateOf("")
    var lastRole by mutableStateOf("")
    var dbUrl by mutableStateOf("")
    var shopCode by mutableStateOf("")
    var myName by mutableStateOf("")
    var shipments by mutableStateOf<List<Shipment>>(emptyList())
    var syncError by mutableStateOf<String?>(null)
    val refreshKey = mutableIntStateOf(0)

    fun configured(): Boolean = dbUrl.isNotBlank() && Cloud.cleanCode(shopCode).isNotEmpty()

    fun saveSettings(r: String, url: String, code: String, name: String) {
        role = r
        dbUrl = url.trim()
        shopCode = Cloud.cleanCode(code)
        myName = name.trim()
        prefs.edit()
            .putString("role", role)
            .putString("url", dbUrl)
            .putString("code", shopCode)
            .putString("name", myName)
            .apply()
        shipments = emptyList()
        syncError = null
        refreshKey.intValue = refreshKey.intValue + 1
    }

    fun init(c: Context) {
        if (!::app.isInitialized) {
            app = c.applicationContext
            db = Db(app)
            db.writableDatabase
            prefs = app.getSharedPreferences("dokon_prefs", Context.MODE_PRIVATE)
            lastRole = prefs.getString("role", "") ?: ""
            // Faqat "Meni eslab qol" belgilangan bo'lsa, kirish ekrani o'tkazib yuboriladi
            role = if (prefs.getBoolean("remember", false) && lastRole.isNotEmpty()) lastRole else ""
            dbUrl = prefs.getString("url", "") ?: ""
            shopCode = prefs.getString("code", "") ?: ""
            myName = prefs.getString("name", "") ?: ""
        }
    }

    fun login(r: String, remember: Boolean) {
        role = r
        lastRole = r
        prefs.edit().putString("role", r).putBoolean("remember", remember).apply()
    }

    fun logout() {
        role = ""
        prefs.edit().putBoolean("remember", false).apply()
    }

    fun bump() {
        tick.intValue = tick.intValue + 1
    }

    /** Zaxira nusxadan tiklash. Muvaffaqiyatli bo'lsa true. */
    fun restore(bytes: ByteArray): Boolean {
        if (bytes.size < 100 || String(bytes, 0, 15, Charsets.US_ASCII) != "SQLite format 3") return false
        val f = db.dbFile()
        val safety = File(app.cacheDir, "pre_restore.db")
        try {
            f.copyTo(safety, overwrite = true)
        } catch (_: Exception) {
        }
        db.close()
        f.writeBytes(bytes)
        File(f.path + "-journal").delete()
        File(f.path + "-wal").delete()
        File(f.path + "-shm").delete()
        return try {
            db = Db(app)
            db.writableDatabase
            db.products()
            SaleDraft.clear()
            bump()
            true
        } catch (_: Exception) {
            try {
                db.close()
                if (safety.exists()) safety.copyTo(f, overwrite = true)
                db = Db(app)
                db.writableDatabase
            } catch (_: Exception) {
            }
            bump()
            false
        }
    }
}

@Composable
fun DokonTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Teal,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCCFBF1),
        onPrimaryContainer = Color(0xFF042F2E),
        secondaryContainer = Color(0xFFCCFBF1),
        onSecondaryContainer = Color(0xFF042F2E),
        background = Color(0xFFF8FAFC),
        surface = Color.White,
        error = Red
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun MoneyField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { ch -> ch.isDigit() }.take(12)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = if (value.isNotEmpty()) ({ Text(fmtMoney(value.toMoney()) + " so'm") }) else null,
        modifier = modifier
    )
}

@Composable
fun QtyField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    decimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(if (decimal) cleanQty(it) else it.filter { ch -> ch.isDigit() }.take(9)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier
    )
}

/** Son maydoni: yonida − va + tugmalari bor. */
@Composable
fun QtyStepper(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    decimal: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onChange(fmtQty(maxOf(0.0, value.toQty() - 1))) }) { Text("−") }
        Spacer(Modifier.width(6.dp))
        QtyField(value, onChange, label, Modifier.weight(1f), decimal)
        Spacer(Modifier.width(6.dp))
        OutlinedButton(onClick = { onChange(fmtQty(value.toQty() + 1)) }) { Text("+") }
    }
}

@Composable
fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = Red) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish") } }
    )
}

@Composable
fun Banner(text: String?, error: Boolean = false) {
    if (text == null) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
        )
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            color = if (error) Red else Green,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ProductPicker(onDismiss: () -> Unit, onPick: (Product) -> Unit, onNew: (() -> Unit)? = null) {
    var q by remember { mutableStateOf("") }
    val all = remember { Store.db.products() }
    val list = remember(q) { filterProducts(all, q) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mahsulotni tanlang") },
        text = {
            Column {
                TextInput(q, { q = it }, "Nomi yoki kodi")
                Spacer(Modifier.height(8.dp))
                if (list.isEmpty()) {
                    Text("Mahsulot topilmadi", color = Muted)
                }
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(list, key = { it.id }) { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(p) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "Qoldiq: ${fmtQty(p.stock)} ${p.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                            }
                            Text(fmtMoney(p.sell))
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            if (onNew != null) TextButton(onClick = onNew) { Text("+ Yangi mahsulot") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Yopish") } }
    )
}

@Composable
fun CustomerPicker(onDismiss: () -> Unit, onPick: (Customer) -> Unit) {
    val list = remember { Store.db.customers() }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mijozni tanlang") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (list.isEmpty()) Text("Hali mijoz yo'q. Pastdan yangisini qo'shing.", color = Muted)
                list.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(c) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(c.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        if (c.debt > 0) Text(fmtMoney(c.debt), color = Red)
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(12.dp))
                Text("Yangi mijoz", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                TextInput(name, { name = it }, "Ismi")
                Spacer(Modifier.height(4.dp))
                TextInput(phone, { phone = it }, "Telefon (ixtiyoriy)")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val n = name.trim()
                    val p = phone.trim()
                    val id = Store.db.addCustomer(n, p)
                    Store.bump()
                    onPick(Customer(id, n, p, 0L))
                }
            }) { Text("Yangisini qo'shish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Yopish") } }
    )
}
