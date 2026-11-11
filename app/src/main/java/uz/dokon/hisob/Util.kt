package uz.dokon.hisob

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor

/** 1250000 -> "1 250 000" (bo'linmas probel bilan) */
fun fmtMoney(v: Long): String {
    val s = Math.abs(v).toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(' ')
        sb.append(s[i])
    }
    return if (v < 0) "-$sb" else sb.toString()
}

/** 2.0 -> "2", 1.5 -> "1.5" */
fun fmtQty(d: Double): String {
    if (d == floor(d) && Math.abs(d) < 1e12) return d.toLong().toString()
    return String.format(Locale.US, "%.3f", d).trimEnd('0').trimEnd('.')
}

fun String.toMoney(): Long = filter { it.isDigit() }.toLongOrNull() ?: 0L

fun String.toQty(): Double = replace(',', '.').toDoubleOrNull() ?: 0.0

/** Miqdor maydoni uchun: faqat raqam va bitta nuqta. */
fun cleanQty(s: String): String {
    var seen = false
    val sb = StringBuilder()
    for (ch in s) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if ((ch == '.' || ch == ',') && !seen) {
            seen = true
            sb.append('.')
        }
    }
    return sb.toString().take(10)
}

fun fmtDate(ts: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date(ts))

fun fileDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

/** daysAgo=0 -> bugun 00:00, 6 -> 6 kun oldingi 00:00 */
fun startOfDay(daysAgo: Int): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    c.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return c.timeInMillis
}

fun lineTotal(price: Long, qty: Double): Long = Math.round(price * qty)

fun filterProducts(all: List<Product>, q: String): List<Product> {
    val t = q.trim()
    if (t.isEmpty()) return all
    return all.filter {
        it.name.contains(t, ignoreCase = true) || it.code.contains(t, ignoreCase = true)
    }
}

/** CSV qatori (Excel uchun nuqtali vergul bilan). */
fun csvLine(vararg cells: Any?): String =
    cells.joinToString(";") { "\"" + (it?.toString() ?: "").replace("\"", "\"\"") + "\"" }
