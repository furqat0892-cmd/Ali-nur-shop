package uz.dokon.hisob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContent {
            DokonTheme {
                Box(
                    Modifier.fillMaxSize().background(
                        if (Store.role.isEmpty()) Brush.verticalGradient(listOf(Color(0xFFCFF1EC), Color(0xFFF8FAFC), Color(0xFFF8FAFC)))
                        else Brush.verticalGradient(listOf(Color(0xFFF1F4F8), Color(0xFFF1F4F8)))
                    )
                ) {
                    if (Store.role.isEmpty()) {
                        LoginScreen()
                    } else {
                        AppRoot(Store.role)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(role: String) {
    // Jo'natmalar va chatni bazadan olib turadi (chat ochiq bo'lsa tezroq)
    LaunchedEffect(Store.dbUrl, Store.shopCode, Store.refreshKey.intValue) {
        if (Store.configured()) {
            val other = if (role == "owner") "receiver" else "owner"
            while (true) {
                try {
                    Store.shipments = Cloud.list()
                    Store.syncError = null
                } catch (e: Exception) {
                    Store.syncError = e.message ?: "Ulanish xatosi"
                }
                try {
                    Store.chat = ChatCloud.list()
                    Store.readOther = ChatCloud.readOf(other)
                } catch (_: Exception) {
                }
                delay(if (Store.chatOpen) 3000L else 8000L)
            }
        }
    }

    val teal1 = Color(0xFF2DD4BF)
    val teal2 = Color(0xFF0F766E)
    val items = if (role == "owner") {
        listOf(
            HomeItem("chat", "Chat", "Sotuvchi bilan yozishma", "\uD83D\uDCAC", teal1, teal2),
            HomeItem("send", "Tovar jo'natish", "Yangi jo'natma yaratish", "\uD83D\uDE9A", Color(0xFFFBBF24), Color(0xFFD97706)),
            HomeItem("history", "Tarix", "Jo'natmalar va qabul", "\uD83D\uDCDC", Color(0xFF60A5FA), Color(0xFF1D4ED8)),
            HomeItem("dekada", "10 kunlik", "Kirim-chiqim hisobi", "\uD83D\uDCC5", Color(0xFFA78BFA), Color(0xFF6D28D9)),
            HomeItem("settings", "Sozlamalar", "Baza, hisob, chiqish", "\u2699\uFE0F", Color(0xFF94A3B8), Color(0xFF334155))
        )
    } else {
        listOf(
            HomeItem("chat", "Chat", "Do'kon egasi bilan", "\uD83D\uDCAC", teal1, teal2),
            HomeItem("sale", "Sotuv", "Yangi chek, nasiya", "\uD83D\uDED2", Color(0xFFFBBF24), Color(0xFFD97706)),
            HomeItem("stock", "Ombor", "Mahsulot, kirim/chiqim", "\uD83D\uDCE6", Color(0xFF60A5FA), Color(0xFF1D4ED8)),
            HomeItem("report", "Hisobot", "Foyda va 10 kunlik", "\uD83D\uDCCA", Color(0xFFA78BFA), Color(0xFF6D28D9)),
            HomeItem("history", "Sotuvlar tarixi", "Cheklar, bekor qilish", "\uD83E\uDDFE", Color(0xFF4ADE80), Color(0xFF15803D)),
            HomeItem("customers", "Mijozlar", "Qarzlar va to'lovlar", "\uD83D\uDC65", Color(0xFFF472B6), Color(0xFFBE185D)),
            HomeItem("backup", "Zaxira", "Saqlash va Excel", "\uD83D\uDCBE", Color(0xFF2DD4BF), Color(0xFF0E7490)),
            HomeItem("settings", "Sozlamalar", "Baza, hisob, chiqish", "\u2699\uFE0F", Color(0xFF94A3B8), Color(0xFF334155))
        )
    }

    var screen by rememberSaveable(role) { mutableStateOf("home") }
    // Telefonning orqaga tugmasi ham bosh sahifaga qaytaradi
    BackHandler(enabled = screen != "home") { screen = "home" }

    // Chatdagi o'qilmagan hodisalar soni (belgi uchun)
    val seen = Store.seenTs
    val unread = if (role == "receiver") {
        Store.chat.count { it.from == "owner" && it.ts > seen } + Store.shipments.count { it.receipt == null }
    } else {
        Store.chat.count { it.from == "receiver" && it.ts > seen } +
            Store.shipments.count { (it.receipt?.ts ?: 0L) > seen }
    }

    val partner = if (role == "owner") "Sotuvchi" else "Do'kon egasi"
    val cur = items.firstOrNull { it.key == screen }
    val headTitle = when {
        screen == "home" -> "Ali nur shop"
        screen == "chat" -> partner
        else -> cur?.title ?: "Ali nur shop"
    }
    val headSub = when {
        screen == "home" -> Auth.roleName(role)
        screen == "chat" -> "Ali nur shop"
        else -> "Ali nur shop \u2022 " + Auth.roleName(role)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            HeaderBar(headTitle, headSub, if (screen == "home") null else ({ screen = "home" }))
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (screen == "home") {
                HomeScreen(items, { key -> if (key == "chat") unread else 0 }, { key -> screen = key })
            } else if (role == "owner") {
                when (screen) {
                    "chat" -> ChatScreen()
                    "send" -> OwnerSend()
                    "history" -> OwnerHistory()
                    "dekada" -> OwnerDekada()
                    else -> SettingsScreen(firstRun = false)
                }
            } else {
                when (screen) {
                    "chat" -> ChatScreen()
                    "sale" -> SaleScreen()
                    "stock" -> OmborScreen()
                    "report" -> ReportScreen()
                    "history" -> MoreScreen(0)
                    "customers" -> MoreScreen(1)
                    "backup" -> MoreScreen(2)
                    else -> SettingsScreen(firstRun = false)
                }
            }
        }
    }
}
