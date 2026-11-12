package uz.dokon.hisob

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Sozlamalar: ism, Firebase baza havolasi va do'kon kodi. Rol endi kirish ekranida tanlanadi. */
@Composable
fun SettingsScreen(firstRun: Boolean = false) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(Store.myName) }
    var url by remember { mutableStateOf(Store.dbUrl) }
    var code by remember { mutableStateOf(Store.shopCode) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        if (url.isNotBlank() && !url.trim().startsWith("https://")) {
            isError = true
            message = "Baza havolasi https:// bilan boshlanishi kerak"
            return false
        }
        return true
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        ScreenTitle("Sozlamalar")
        Banner(message, isError)
        Text(
            "Rol: " + Auth.roleName(Store.role),
            color = Muted,
            style = MaterialTheme.typography.bodyMedium
        )
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
                    Store.saveSettings(Store.role, url, code, name)
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
                    Store.saveSettings(Store.role, url, code, name)
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

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { Store.logout() }
        ) { Text("Chiqish (kirish ekraniga qaytish)") }
        Spacer(Modifier.height(24.dp))
    }
}
