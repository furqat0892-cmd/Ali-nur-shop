package uz.dokon.hisob

import android.os.Bundle
import androidx.activity.ComponentActivity
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

private data class NavTab(val label: String, val icon: ImageVector)

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

    val tabs = if (role == "owner") {
        listOf(
            NavTab("Chat", Icons.Filled.Email),
            NavTab("Jo'natish", Icons.Filled.Send),
            NavTab("Tarix", Icons.Filled.Home),
            NavTab("10 kunlik", Icons.Filled.Info),
            NavTab("Sozlamalar", Icons.Filled.Settings)
        )
    } else {
        listOf(
            NavTab("Chat", Icons.Filled.Email),
            NavTab("Sotuv", Icons.Filled.ShoppingCart),
            NavTab("Ombor", Icons.Filled.Home),
            NavTab("Hisobot", Icons.Filled.Info),
            NavTab("Yana", Icons.Filled.Menu)
        )
    }
    var tab by rememberSaveable(role) { mutableIntStateOf(0) }

    // Chatdagi o'qilmagan hodisalar soni (belgi uchun)
    val seen = Store.seenTs
    val unread = if (role == "receiver") {
        Store.chat.count { it.from == "owner" && it.ts > seen } + Store.shipments.count { it.receipt == null }
    } else {
        Store.chat.count { it.from == "receiver" && it.ts > seen } +
            Store.shipments.count { (it.receipt?.ts ?: 0L) > seen }
    }
    val showUnread = if (tab == 0 && role == "owner") 0 else unread

    val partner = if (role == "owner") "Sotuvchi" else "Do'kon egasi"
    val headTitle = if (tab == 0) partner else tabs[tab].label
    val headSub = if (tab == 0) "Ali nur shop" else "Ali nur shop \u2022 " + Auth.roleName(role)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { HeaderBar(headTitle, headSub) },
        bottomBar = {
            if (!(tab == 0 && Store.typing)) {
                NavigationBar(
                    modifier = Modifier.shadow(8.dp),
                    containerColor = Color.White
                ) {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = {
                                if (i == 0 && showUnread > 0) {
                                    BadgedBox(badge = { Badge { Text(showUnread.toString()) } }) {
                                        Icon(t.icon, contentDescription = t.label)
                                    }
                                } else {
                                    Icon(t.icon, contentDescription = t.label)
                                }
                            },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (role == "owner") {
                when (tab) {
                    0 -> ChatScreen()
                    1 -> OwnerSend()
                    2 -> OwnerHistory()
                    3 -> OwnerDekada()
                    else -> SettingsScreen(firstRun = false)
                }
            } else {
                when (tab) {
                    0 -> ChatScreen()
                    1 -> SaleScreen()
                    2 -> OmborScreen()
                    3 -> ReportScreen()
                    else -> MoreScreen()
                }
            }
        }
    }
}
