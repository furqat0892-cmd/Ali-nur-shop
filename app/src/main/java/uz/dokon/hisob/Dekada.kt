package uz.dokon.hisob

import java.util.Calendar

class Dekada(val label: String, val start: Long, val end: Long)

private val MONTHS = listOf(
    "Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
    "Iyul", "Avgust", "Sentabr", "Oktabr", "Noyabr", "Dekabr"
)

/**
 * Oxirgi [count] ta "dekada" (10 kunlik davr): 1-10, 11-20, 21-oy oxiri.
 * Eng yangisi birinchi. end - keyingi davrning boshlanishi (kiritilmaydi).
 */
fun dekadas(count: Int): List<Dekada> {
    val out = ArrayList<Dekada>()
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    val today = c.get(Calendar.DAY_OF_MONTH)
    c.set(Calendar.DAY_OF_MONTH, if (today <= 10) 1 else if (today <= 20) 11 else 21)
    for (n in 0 until count) {
        val start = c.timeInMillis
        val day = c.get(Calendar.DAY_OF_MONTH)
        val e = c.clone() as Calendar
        val month = MONTHS[c.get(Calendar.MONTH)]
        val year = c.get(Calendar.YEAR)
        val label: String
        if (day == 1) {
            e.set(Calendar.DAY_OF_MONTH, 11)
            label = "1–10 $month $year"
        } else if (day == 11) {
            e.set(Calendar.DAY_OF_MONTH, 21)
            label = "11–20 $month $year"
        } else {
            val last = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            e.set(Calendar.DAY_OF_MONTH, 1)
            e.add(Calendar.MONTH, 1)
            label = "21–$last $month $year"
        }
        out.add(Dekada(label, start, e.timeInMillis))
        if (day == 21) {
            c.set(Calendar.DAY_OF_MONTH, 11)
        } else if (day == 11) {
            c.set(Calendar.DAY_OF_MONTH, 1)
        } else {
            c.add(Calendar.MONTH, -1)
            c.set(Calendar.DAY_OF_MONTH, 21)
        }
    }
    return out
}
