package uz.dokon.hisob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContent {
            DokonTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            val items = listOf(
                                Triple("Sotuv", Icons.Filled.ShoppingCart, 0),
                                Triple("Ombor", Icons.Filled.Home, 1),
                                Triple("Kirim", Icons.Filled.Add, 2),
                                Triple("Hisobot", Icons.Filled.Info, 3),
                                Triple("Yana", Icons.Filled.Menu, 4)
                            )
                            items.forEach { (label, icon, i) ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                ) { pad ->
                    Box(Modifier.padding(pad).fillMaxSize()) {
                        when (tab) {
                            0 -> SaleScreen()
                            1 -> ProductsScreen()
                            2 -> StockScreen()
                            3 -> ReportScreen()
                            else -> MoreScreen()
                        }
                    }
                }
            }
        }
    }
}
