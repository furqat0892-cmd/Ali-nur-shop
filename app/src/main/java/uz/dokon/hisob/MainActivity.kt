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
                        Brush.verticalGradient(listOf(Color(0xFFCFF1EC), Color(0xFFF8FAFC), Color(0xFFF8FAFC)))
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
    // Do'kon egasi jo'natgan tovarlarni har 10 soniyada bazadan olib turadi
    LaunchedEffect(Store.dbUrl, Store.shopCode, Store.refreshKey.intValue) {
        if (Store.configured()) {
            while (true) {
                try {
                    Store.shipments = Cloud.list()
                    Store.syncError = null
                } catch (e: Exception) {
                    Store.syncError = e.message ?: "Ulanish xatosi"
                }
                delay(10000)
            }
        }
    }

    val tabs = if (role == "owner") {
        listOf(
            NavTab("Jo'natish", Icons.Filled.Send),
            NavTab("Tarix", Icons.Filled.Home),
            NavTab("10 kunlik", Icons.Filled.Info),
            NavTab("Sozlamalar", Icons.Filled.Settings)
        )
    } else {
        listOf(
            NavTab("Sotuv", Icons.Filled.ShoppingCart),
            NavTab("Ombor", Icons.Filled.Home),
            NavTab("Xabarlar", Icons.Filled.Email),
            NavTab("Hisobot", Icons.Filled.Info),
            NavTab("Yana", Icons.Filled.Menu)
        )
    }
    var tab by rememberSaveable(role) { mutableIntStateOf(0) }
    val pending = Store.shipments.count { it.receipt == null }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.shadow(16.dp),
                containerColor = Color.White
            ) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            if (role == "receiver" && t.label == "Xabarlar" && pending > 0) {
                                BadgedBox(badge = { Badge { Text(pending.toString()) } }) {
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
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (role == "owner") {
                when (tab) {
                    0 -> OwnerSend()
                    1 -> OwnerHistory()
                    2 -> OwnerDekada()
                    else -> SettingsScreen(firstRun = false)
                }
            } else {
                when (tab) {
                    0 -> SaleScreen()
                    1 -> OmborScreen()
                    2 -> InboxScreen()
                    3 -> ReportScreen()
                    else -> MoreScreen()
                }
            }
        }
    }
}
