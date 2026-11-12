package uz.dokon.hisob

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ShipItem(val name: String, val qty: Double, val unit: String, val price: Long)

data class ReceiptItem(
    val name: String,
    val sent: Double,
    val ok: Double,
    val damaged: Double,
    val missing: Double,
    val price: Long
)

data class Receipt(val ts: Long, val note: String, val items: List<ReceiptItem>) {
    val allOk: Boolean
        get() = items.all { it.damaged <= 0.0005 && it.missing <= 0.0005 }
    val lossValue: Long
        get() = items.sumOf { lineTotal(it.price, it.damaged + it.missing) }
    val okValue: Long
        get() = items.sumOf { lineTotal(it.price, it.ok) }
}

data class Shipment(
    val id: String,
    val ts: Long,
    val sender: String,
    val note: String,
    val items: List<ShipItem>,
    val receipt: Receipt?
) {
    val total: Long
        get() = items.sumOf { lineTotal(it.price, it.qty) }
}

fun Shipment.toJson(): JSONObject {
    val o = JSONObject()
    o.put("id", id)
    o.put("ts", ts)
    o.put("sender", sender)
    o.put("note", note)
    val arr = JSONArray()
    for (i in items) {
        val j = JSONObject()
        j.put("n", i.name)
        j.put("q", i.qty)
        j.put("u", i.unit)
        j.put("p", i.price)
        arr.put(j)
    }
    o.put("items", arr)
    val r = receipt
    if (r != null) {
        val ro = JSONObject()
        ro.put("ts", r.ts)
        ro.put("note", r.note)
        val ra = JSONArray()
        for (i in r.items) {
            val j = JSONObject()
            j.put("n", i.name)
            j.put("s", i.sent)
            j.put("ok", i.ok)
            j.put("d", i.damaged)
            j.put("m", i.missing)
            j.put("p", i.price)
            ra.put(j)
        }
        ro.put("items", ra)
        o.put("receipt", ro)
    }
    return o
}

fun parseShipment(o: JSONObject, fallbackId: String): Shipment {
    val items = ArrayList<ShipItem>()
    val arr = o.optJSONArray("items")
    if (arr != null) {
        for (i in 0 until arr.length()) {
            val j = arr.optJSONObject(i) ?: continue
            items.add(ShipItem(j.optString("n"), j.optDouble("q", 0.0), j.optString("u", "dona"), j.optLong("p", 0L)))
        }
    }
    var receipt: Receipt? = null
    val ro = o.optJSONObject("receipt")
    if (ro != null) {
        val ri = ArrayList<ReceiptItem>()
        val ra = ro.optJSONArray("items")
        if (ra != null) {
            for (i in 0 until ra.length()) {
                val j = ra.optJSONObject(i) ?: continue
                ri.add(
                    ReceiptItem(
                        j.optString("n"), j.optDouble("s", 0.0), j.optDouble("ok", 0.0),
                        j.optDouble("d", 0.0), j.optDouble("m", 0.0), j.optLong("p", 0L)
                    )
                )
            }
        }
        receipt = Receipt(ro.optLong("ts", 0L), ro.optString("note"), ri)
    }
    val id = o.optString("id").ifEmpty { fallbackId }
    return Shipment(id, o.optLong("ts", 0L), o.optString("sender"), o.optString("note"), items, receipt)
}

/**
 * Firebase Realtime Database bilan REST orqali ishlaydi (qo'shimcha kutubxona kerak emas).
 * Ma'lumot yo'li: <baza>/shops/<do'kon kodi>/shipments/<id>.json
 */
object Cloud {
    fun cleanCode(s: String): String =
        s.trim().filter { it.isLetterOrDigit() || it == '-' || it == '_' }

    private fun url(id: String?): String {
        val base = Store.dbUrl.trim().trimEnd('/')
        val code = cleanCode(Store.shopCode)
        return base + "/shops/" + code + "/shipments" + (if (id != null) "/$id" else "") + ".json"
    }

    fun call(method: String, u: String, body: String?): String {
        val c = URL(u).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 10000
            c.readTimeout = 15000
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val hint = if (code == 401 || code == 403) " (bazaga ruxsat yo'q: Firebase Rules ni tekshiring)" else ""
                throw Exception("Server xatosi $code$hint")
            }
            return text
        } finally {
            c.disconnect()
        }
    }

    suspend fun list(): List<Shipment> = withContext(Dispatchers.IO) {
        val text = call("GET", url(null), null).trim()
        val out = ArrayList<Shipment>()
        if (text.isNotEmpty() && text != "null") {
            val obj = JSONObject(text)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = obj.optJSONObject(k)
                if (o != null) out.add(parseShipment(o, k))
            }
        }
        out.sortedByDescending { it.ts }
    }

    suspend fun put(s: Shipment) {
        withContext(Dispatchers.IO) {
            call("PUT", url(s.id), s.toJson().toString())
        }
    }
}
