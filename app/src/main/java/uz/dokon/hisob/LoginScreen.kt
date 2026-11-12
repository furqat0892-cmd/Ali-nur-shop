package uz.dokon.hisob

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Kirish ekrani (tepadan pastga):
 * brend rasmi, 1) Do'kon egasi, 2) Sotuvchi, 3) Gmail, 4) parol.
 * Gmail va parol Firebase Authentication orqali tekshiriladi. Parol unutilsa Gmail'ga havola yuboriladi.
 */
@Composable
fun LoginScreen() {
    val scope = rememberCoroutineScope()
    var role by remember { mutableStateOf(Store.lastRole) }
    var email by remember { mutableStateOf(Store.prefs.getString("email", "") ?: "") }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var keep by remember { mutableStateOf(true) }
    var register by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    fun pickRole(r: String) {
        role = r
        error = null
        info = null
    }

    fun mail(): String = email.trim().lowercase()

    fun check(needPw: Boolean): Boolean {
        info = null
        if (role.isEmpty()) {
            error = "Avval rolni tanlang: Do'kon egasi yoki Sotuvchi"
            return false
        }
        val m = mail()
        if (!m.contains("@") || !m.contains(".") || m.length < 6) {
            error = "Gmail manzilini to'g'ri yozing (masalan: ism@gmail.com)"
            return false
        }
        if (needPw) {
            if (pw.length < 6) {
                error = "Parol kamida 6 belgidan iborat bo'lsin"
                return false
            }
            if (register && pw != pw2) {
                error = "Ikkala parol bir xil emas"
                return false
            }
        }
        error = null
        return true
    }

    fun finish() {
        Store.prefs.edit().putString("email", mail()).apply()
        Store.login(role, keep)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BrandHero()
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                if (register) "Ro'yxatdan o'tish" else "Kirish",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Banner(error, true)
            Banner(info, false)

            // 1-qator va 2-qator: rol
            RoleRow("Do'kon egasi", "Tovar jo'natadi, qabul natijasini ko'radi", role == "owner") { pickRole("owner") }
            RoleRow("Sotuvchi", "Tovar qabul qiladi, sotadi, omborni yuritadi", role == "receiver") { pickRole("receiver") }
            Spacer(Modifier.height(10.dp))

            // 3-qator: Gmail
            TextInput(email, { email = it }, "Gmail manzili", keyboard = KeyboardType.Email)
            Spacer(Modifier.height(8.dp))
            // 4-qator: parol
            PasswordField(pw, { pw = it }, if (register) "Yangi parol (kamida 6 belgi)" else "Parol", showPw) { showPw = !showPw }
            if (register) {
                Spacer(Modifier.height(8.dp))
                PasswordField(pw2, { pw2 = it }, "Parolni takrorlang", showPw) { showPw = !showPw }
                Text(
                    "Gmail'ingizga tasdiqlash havolasi yuboriladi. Uni bosgandan keyin kira olasiz.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        if (check(false)) {
                            busy = true
                            scope.launch {
                                try {
                                    GAuth.resetPassword(mail())
                                    info = "Parolni tiklash havolasi ${mail()} manziliga yuborildi. Pochtangizni (Spam ham) tekshiring"
                                } catch (e: Exception) {
                                    error = e.message ?: "Xatolik"
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    }
                ) { Text("Parolni unutdingizmi? Gmail'ga havola yuborish") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = keep, onCheckedChange = { keep = it })
                Text("Meni eslab qol", modifier = Modifier.clickable { keep = !keep })
            }
            Spacer(Modifier.height(8.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                onClick = {
                    if (check(true)) {
                        busy = true
                        scope.launch {
                            try {
                                if (register) {
                                    GAuth.signUp(mail(), pw)
                                    register = false
                                    pw = ""
                                    pw2 = ""
                                    info = "Ro'yxatdan o'tdingiz. ${mail()} manziliga tasdiqlash havolasi yuborildi: uni bosing, so'ng shu yerdan kiring"
                                } else {
                                    GAuth.signIn(mail(), pw)
                                    finish()
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Xatolik"
                            } finally {
                                busy = false
                            }
                        }
                    }
                }
            ) { Text(if (busy) "Iltimos kuting..." else if (register) "Ro'yxatdan o'tish" else "Kirish") }
            TextButton(onClick = {
                register = !register
                error = null
                info = null
                pw = ""
                pw2 = ""
            }) { Text(if (register) "Hisobim bor. Kirish" else "Birinchi martami? Ro'yxatdan o'tish") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BrandHero() {
    val shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
    Image(
        painter = painterResource(R.drawable.brand_hero),
        contentDescription = "Ali nur shop",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .shadow(14.dp, shape)
    )
}

@Composable
private fun RoleRow(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFCCFBF1) else Color.White)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) Teal else Color.Unspecified
                )
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            if (selected) {
                Text("✓", color = Teal, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    show: Boolean,
    onToggle: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { TextButton(onClick = onToggle) { Text(if (show) "Yashirish" else "Ko'rsatish") } },
        modifier = Modifier.fillMaxWidth()
    )
}
