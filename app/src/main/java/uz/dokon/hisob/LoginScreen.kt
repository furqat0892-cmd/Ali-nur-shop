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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Kirish ekrani (tepadan pastga):
 * brend rasmi, 1) Do'kon egasi, 2) Sotuvchi, 3) login, 4) parol, parolni tiklash.
 */
@Composable
fun LoginScreen() {
    var role by remember { mutableStateOf(Store.lastRole) }
    var login by remember { mutableStateOf(Auth.LOGIN) }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var keep by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    // 0 = oddiy kirish, 1 = tiklash kodini kiritish, 2 = yangi parol o'rnatish
    var recover by remember { mutableIntStateOf(0) }
    var codeInput by remember { mutableStateOf("") }
    var shownCode by remember { mutableStateOf<String?>(null) }

    val roleSet = role.isNotEmpty() && Auth.isSet(role)
    val setup = role.isNotEmpty() && !roleSet

    fun pickRole(r: String) {
        role = r
        error = null
        pw = ""
        pw2 = ""
        recover = 0
        codeInput = ""
    }

    fun loginOk(): Boolean {
        if (role.isEmpty()) {
            error = "Avval rolni tanlang: Do'kon egasi yoki Sotuvchi"
            return false
        }
        if (login.trim().lowercase() != Auth.LOGIN) {
            error = "Login noto'g'ri"
            return false
        }
        return true
    }

    fun newPasswordOk(): Boolean {
        if (pw.length < 6) {
            error = "Parol kamida 6 belgidan iborat bo'lsin"
            return false
        }
        if (pw != pw2) {
            error = "Ikkala parol bir xil emas"
            return false
        }
        if (Auth.sameAsOther(role, pw)) {
            error = "Bu parol boshqa rol uchun ishlatilgan. Do'kon egasi va Sotuvchi parollari har xil bo'lishi kerak"
            return false
        }
        return true
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BrandHero()
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                when (recover) {
                    1 -> "Parolni tiklash"
                    2 -> "Yangi parol"
                    else -> "Kirish"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Banner(error, true)

            // 1-qator va 2-qator: rol
            RoleRow("Do'kon egasi", "Tovar jo'natadi, qabul natijasini ko'radi", role == "owner") { pickRole("owner") }
            RoleRow("Sotuvchi", "Tovar qabul qiladi, sotadi, omborni yuritadi", role == "receiver") { pickRole("receiver") }
            Spacer(Modifier.height(10.dp))

            if (recover == 1) {
                Text(
                    "${Auth.roleName(role)} parolini o'rnatganingizda berilgan tiklash kodini kiriting (XXXX-XXXX).",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                TextInput(codeInput, { codeInput = it.uppercase() }, "Tiklash kodi")
                Spacer(Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (Auth.checkRecovery(role, codeInput)) {
                            recover = 2
                            error = null
                            pw = ""
                            pw2 = ""
                        } else {
                            error = "Tiklash kodi noto'g'ri"
                        }
                    }
                ) { Text("Davom etish") }
                TextButton(onClick = {
                    recover = 0
                    error = null
                }) { Text("← Orqaga") }
            } else if (recover == 2) {
                PasswordField(pw, { pw = it }, "Yangi parol", showPw) { showPw = !showPw }
                Spacer(Modifier.height(8.dp))
                PasswordField(pw2, { pw2 = it }, "Yangi parolni takrorlang", showPw) { showPw = !showPw }
                Spacer(Modifier.height(12.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (newPasswordOk()) shownCode = Auth.setPassword(role, pw)
                    }
                ) { Text("Yangi parolni saqlash") }
            } else {
                if (setup) {
                    Text(
                        "${Auth.roleName(role)} uchun parol hali o'rnatilmagan. Yangi parol o'ylab toping.",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }
                // 3-qator: login
                TextInput(login, { login = it }, "Login")
                Spacer(Modifier.height(8.dp))
                // 4-qator: parol
                PasswordField(pw, { pw = it }, if (setup) "Yangi parol" else "Parol", showPw) { showPw = !showPw }
                if (setup) {
                    Spacer(Modifier.height(8.dp))
                    PasswordField(pw2, { pw2 = it }, "Parolni takrorlang", showPw) { showPw = !showPw }
                }
                if (roleSet) {
                    TextButton(onClick = {
                        recover = 1
                        error = null
                        pw = ""
                    }) { Text("Parolni unutdingizmi? Tiklash") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = keep, onCheckedChange = { keep = it })
                    Text("Meni eslab qol", modifier = Modifier.clickable { keep = !keep })
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (loginOk()) {
                            if (setup) {
                                if (newPasswordOk()) shownCode = Auth.setPassword(role, pw)
                            } else if (Auth.check(role, pw)) {
                                Store.login(role, keep)
                            } else {
                                error = "Parol noto'g'ri"
                            }
                        }
                    }
                ) { Text(if (setup) "Parolni o'rnatish" else "Kirish") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    shownCode?.let { code ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Tiklash kodingiz") },
            text = {
                Column {
                    Text(
                        "Parolni unutib qo'ysangiz, faqat shu kod bilan tiklay olasiz. Uni qog'ozga yozib, xavfsiz joyda saqlang. Kod faqat hozir, bir marta ko'rsatiladi."
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        code,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Teal
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    shownCode = null
                    Store.login(role, keep)
                }) { Text("Yozib oldim, kirish") }
            }
        )
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
