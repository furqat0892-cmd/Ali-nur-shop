package uz.dokon.hisob

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(firstRun: Boolean) {
    val scope = rememberCoroutineScope()
    var role by remember { mutableStateOf(Store.role) }
    var name by remember { mutableStateOf(Store.myName) }
    var url by remember { mutableStateOf(Store.dbUrl) }
    var code by remember { mutableStateOf(Store.shopCode) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        if (role.isEmpty()) {
            isError = true
            message = "Rolni tanlang"
            return false
        }
        if (url.isNotBlank() && !url.trim().startsWith("https://")) {
            isError = true
            message = "Baza havolasi https:// bilan boshlanishi kerak"
            return false
        }
        return true
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        ScreenTitle(if (firstRun) "Xush kelibsiz" else "Sozlamalar")
        Banner(message, isError)
        Text("Siz kimsiz?", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        RoleCard(
            title = "Do'kon egasi",
            desc = "Tovar jo'natadi va qabul natijasini ko'radi",
            selected = role == "owner"
        ) { role = "owner" }
        RoleCard(
            title = "Qabul qiluvchi (do'kon)",
            desc = "Jo'natilgan tovarni qabul qiladi, sotuv va omborni yuritadi",
            selected = role == "receiver"
        ) { role = "receiver" }

        Spacer(Modifier.height(12.dp))
        TextInput(name, { name = it }, "Ismingiz (xabarlarda ko'rinadi)")
        Spacer(Modifier.height(8.dp))
        TextInput(url, { url = it }, "Baza havolasi (Firebase Realtime Database)")
        Spacer(Modifier.height(8.dp))
        TextInput(code, { code = it }, "Do'kon kodi (ikkala telefonda bir xil)")
        Text(
            "Do'kon kodi parol vazifasini bajaradi: uzunroq va tasodifiy qiling (masalan: alinur-7392-xk). Faqat harf, raqam, - va _ ishlatiladi.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(14.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (validate()) {
                    Store.saveSettings(role, url, code, name)
                    isError = false
                    message = "Saqlandi"
                }
            }
        ) { Text("Saqlash") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !testing,
            onClick = {
                if (validate()) {
                    Store.saveSettings(role, url, code, name)
                    if (!Store.configured()) {
                        isError = true
                        message = "Baza havolasi va do'kon kodini kiriting"
                    } else {
                        testing = true
                        scope.launch {
                            try {
                                val n = Cloud.list().size
                                isError = false
                                message = "Ulanish muvaffaqiyatli. Bazada $n ta jo'natma bor."
                            } catch (e: Exception) {
                                isError = true
                                message = "Ulanib bo'lmadi: ${e.message}"
                            } finally {
                                testing = false
                            }
                        }
                    }
                }
            }
        ) { Text(if (testing) "Tekshirilmoqda..." else "Ulanishni tekshirish") }

        if (firstRun) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Bazani keyinroq ham sozlashingiz mumkin: faqat rolni tanlab Saqlash bosing.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RoleCard(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) androidx.compose.ui.graphics.Color(0xFFCCFBF1) else androidx.compose.ui.graphics.Color.White
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                (if (selected) "✓ " else "") + title,
                fontWeight = FontWeight.Bold,
                color = if (selected) Teal else androidx.compose.ui.graphics.Color.Unspecified
            )
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}
