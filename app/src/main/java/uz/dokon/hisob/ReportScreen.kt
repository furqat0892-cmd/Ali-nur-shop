package uz.dokon.hisob

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReportScreen() {
    val tick = Store.tick.intValue
    var period by remember { mutableIntStateOf(0) }
    val labels = listOf("Bugun", "7 kun", "30 kun")
    val since = remember(period) {
        when (period) {
            0 -> startOfDay(0)
            1 -> startOfDay(6)
            else -> startOfDay(29)
        }
    }
    val r = remember(period, tick) { Store.db.report(since) }
    val lowStock = remember(tick) { Store.db.products().filter { it.stock <= it.minStock } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        ScreenTitle("Hisobot")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { i, l ->
                FilterChip(selected = period == i, onClick = { period = i }, label = { Text(l) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Savdo", fmtMoney(r.revenue) + " so'm", Modifier.weight(1f))
            StatCard("Foyda", fmtMoney(r.profit) + " so'm", Modifier.weight(1f), if (r.profit >= 0) Green else Red)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Cheklar soni", r.count.toString(), Modifier.weight(1f))
            StatCard("Qo'lga tushgan pul", fmtMoney(r.cash) + " so'm", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Yangi nasiya", fmtMoney(r.newDebt) + " so'm", Modifier.weight(1f), if (r.newDebt > 0) Red else Color0)
            StatCard("Tovar xaridi", fmtMoney(r.purchases) + " so'm", Modifier.weight(1f))
        }
        if (r.writeoff > 0) {
            Spacer(Modifier.height(8.dp))
            StatCard("Chiqim (zarar)", fmtMoney(r.writeoff) + " so'm", Modifier.fillMaxWidth(), Red)
        }

        Spacer(Modifier.height(16.dp))
        Text("Eng ko'p sotilgan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (r.top.isEmpty()) {
            Text("Bu davrda sotuv yo'q", color = Muted, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    r.top.forEachIndexed { i, (name, sum) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}. $name", Modifier.weight(1f))
                            Text(fmtMoney(sum), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Kam qolgan mahsulotlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (lowStock.isEmpty()) {
            Text("Hammasi yetarli", color = Muted, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    lowStock.take(30).forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(p.name, Modifier.weight(1f))
                            Text(fmtQty(p.stock) + " " + p.unit, color = Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private val Color0 = androidx.compose.ui.graphics.Color.Unspecified
