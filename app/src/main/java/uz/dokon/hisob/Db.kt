package uz.dokon.hisob

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

data class Product(
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val category: String = "",
    val unit: String = "dona",
    val buy: Long = 0,
    val sell: Long = 0,
    val stock: Double = 0.0,
    val minStock: Double = 0.0
)

data class Customer(val id: Long, val name: String, val phone: String, val debt: Long)

data class CartLine(val product: Product, val qty: Double, val price: Long)

data class SaleRow(
    val id: Long,
    val ts: Long,
    val total: Long,
    val paid: Long,
    val method: String,
    val customer: String,
    val cancelled: Boolean,
    val items: String
)

data class MoveRow(
    val id: Long,
    val ts: Long,
    val kind: String,
    val productName: String,
    val unit: String,
    val qty: Double,
    val price: Long,
    val note: String
)

data class Report(
    val count: Int,
    val revenue: Long,
    val profit: Long,
    val cash: Long,
    val newDebt: Long,
    val purchases: Long,
    val writeoff: Long,
    val top: List<Pair<String, Long>>
)

data class StockSummary(val count: Int, val costValue: Long, val sellValue: Long)

private const val PCOLS = "id, name, code, category, unit, buy, sell, stock, min_stock"

private fun Cursor.toProduct() = Product(
    id = getLong(0),
    name = getString(1),
    code = getString(2),
    category = getString(3),
    unit = getString(4),
    buy = getLong(5),
    sell = getLong(6),
    stock = getDouble(7),
    minStock = getDouble(8)
)

class Db(private val ctx: Context) : SQLiteOpenHelper(ctx, NAME, null, 1) {

    companion object {
        const val NAME = "dokon.db"
        const val INITIAL_NOTE = "Boshlang'ich qoldiq"
        const val ADJUST_NOTE = "Qoldiq tuzatildi"
    }

    fun dbFile(): File = ctx.getDatabasePath(NAME)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, " +
                "code TEXT NOT NULL DEFAULT '', category TEXT NOT NULL DEFAULT '', " +
                "unit TEXT NOT NULL DEFAULT 'dona', buy INTEGER NOT NULL DEFAULT 0, " +
                "sell INTEGER NOT NULL DEFAULT 0, stock REAL NOT NULL DEFAULT 0, " +
                "min_stock REAL NOT NULL DEFAULT 0, active INTEGER NOT NULL DEFAULT 1)"
        )
        db.execSQL(
            "CREATE TABLE customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, " +
                "phone TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL(
            "CREATE TABLE sales (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, " +
                "total INTEGER NOT NULL, paid INTEGER NOT NULL, method TEXT NOT NULL, " +
                "customer_id INTEGER, cancelled INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE sale_items (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER NOT NULL, " +
                "product_id INTEGER NOT NULL, qty REAL NOT NULL, price INTEGER NOT NULL, cost INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE moves (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, " +
                "kind TEXT NOT NULL, product_id INTEGER NOT NULL, qty REAL NOT NULL, " +
                "price INTEGER NOT NULL, supplier TEXT NOT NULL DEFAULT '', note TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL(
            "CREATE TABLE debt_payments (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, " +
                "customer_id INTEGER NOT NULL, amount INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_sales_ts ON sales(ts)")
        db.execSQL("CREATE INDEX idx_moves_ts ON moves(ts)")
        db.execSQL("CREATE INDEX idx_items_sale ON sale_items(sale_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    // ---------------------------------------------------------------- Mahsulotlar

    fun products(): List<Product> {
        val out = ArrayList<Product>()
        readableDatabase.rawQuery(
            "SELECT $PCOLS FROM products WHERE active=1 ORDER BY name COLLATE NOCASE", null
        ).use { c -> while (c.moveToNext()) out.add(c.toProduct()) }
        return out
    }

    fun codeExists(code: String, excludeId: Long): Boolean {
        var found = false
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM products WHERE active=1 AND code=? AND id<>?",
            arrayOf(code, excludeId.toString())
        ).use { if (it.moveToFirst()) found = it.getInt(0) > 0 }
        return found
    }

    private fun rawProduct(db: SQLiteDatabase, id: Long): Product? =
        db.rawQuery("SELECT $PCOLS FROM products WHERE id=?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) it.toProduct() else null
        }

    private fun insertMove(
        db: SQLiteDatabase, kind: String, pid: Long, qty: Double,
        price: Long, supplier: String, note: String
    ) {
        val cv = ContentValues()
        cv.put("ts", System.currentTimeMillis())
        cv.put("kind", kind)
        cv.put("product_id", pid)
        cv.put("qty", qty)
        cv.put("price", price)
        cv.put("supplier", supplier)
        cv.put("note", note)
        db.insertOrThrow("moves", null, cv)
    }

    fun insertProduct(p: Product): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues()
            cv.put("name", p.name)
            cv.put("code", p.code)
            cv.put("category", p.category)
            cv.put("unit", p.unit)
            cv.put("buy", p.buy)
            cv.put("sell", p.sell)
            cv.put("stock", p.stock)
            cv.put("min_stock", p.minStock)
            val id = db.insertOrThrow("products", null, cv)
            if (p.stock > 0) insertMove(db, "IN", id, p.stock, p.buy, "", INITIAL_NOTE)
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    fun updateProduct(p: Product) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val old = rawProduct(db, p.id)
            val cv = ContentValues()
            cv.put("name", p.name)
            cv.put("code", p.code)
            cv.put("category", p.category)
            cv.put("unit", p.unit)
            cv.put("buy", p.buy)
            cv.put("sell", p.sell)
            cv.put("min_stock", p.minStock)
            db.update("products", cv, "id=?", arrayOf(p.id.toString()))
            if (old != null) {
                val diff = p.stock - old.stock
                if (Math.abs(diff) > 0.0005) {
                    insertMove(db, if (diff > 0) "IN" else "OUT", p.id, Math.abs(diff), p.buy, "", ADJUST_NOTE)
                    db.execSQL("UPDATE products SET stock = ? WHERE id = ?", arrayOf<Any>(p.stock, p.id))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Mahsulotni arxivga o'tkazadi (tarix saqlanib qoladi). */
    fun archiveProduct(id: Long) {
        val cv = ContentValues()
        cv.put("active", 0)
        writableDatabase.update("products", cv, "id=?", arrayOf(id.toString()))
    }

    fun stockSummary(): StockSummary {
        var count = 0
        var cost = 0.0
        var sell = 0.0
        readableDatabase.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(stock*buy),0), COALESCE(SUM(stock*sell),0) " +
                "FROM products WHERE active=1 AND stock>0", null
        ).use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
                cost = it.getDouble(1)
                sell = it.getDouble(2)
            }
        }
        return StockSummary(count, Math.round(cost), Math.round(sell))
    }

    // ---------------------------------------------------------------- Kirim / chiqim

    fun stockIn(pid: Long, qty: Double, price: Long, supplier: String, newSell: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            insertMove(db, "IN", pid, qty, price, supplier, "")
            db.execSQL(
                "UPDATE products SET stock = stock + ?, buy = ?, sell = ? WHERE id = ?",
                arrayOf<Any>(qty, price, newSell, pid)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun stockOut(pid: Long, qty: Double, note: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cost = rawProduct(db, pid)?.buy ?: 0L
            insertMove(db, "OUT", pid, qty, cost, "", note)
            db.execSQL("UPDATE products SET stock = stock - ? WHERE id = ?", arrayOf<Any>(qty, pid))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteMove(id: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var kind = ""
            var pid = 0L
            var qty = 0.0
            db.rawQuery("SELECT kind, product_id, qty FROM moves WHERE id=?", arrayOf(id.toString())).use {
                if (it.moveToFirst()) {
                    kind = it.getString(0)
                    pid = it.getLong(1)
                    qty = it.getDouble(2)
                }
            }
            if (kind.isNotEmpty()) {
                val delta = if (kind == "IN") -qty else qty
                db.execSQL("UPDATE products SET stock = stock + ? WHERE id = ?", arrayOf<Any>(delta, pid))
                db.delete("moves", "id=?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun moves(limit: Int): List<MoveRow> {
        val out = ArrayList<MoveRow>()
        readableDatabase.rawQuery(
            "SELECT m.id, m.ts, m.kind, p.name, p.unit, m.qty, m.price, m.supplier, m.note " +
                "FROM moves m JOIN products p ON p.id=m.product_id " +
                "ORDER BY m.ts DESC, m.id DESC LIMIT $limit", null
        ).use { c ->
            while (c.moveToNext()) {
                val note = listOf(c.getString(7), c.getString(8)).filter { it.isNotBlank() }.joinToString(" • ")
                out.add(
                    MoveRow(
                        c.getLong(0), c.getLong(1), c.getString(2), c.getString(3),
                        c.getString(4), c.getDouble(5), c.getLong(6), note
                    )
                )
            }
        }
        return out
    }

    // ---------------------------------------------------------------- Sotuv

    fun createSale(lines: List<CartLine>, method: String, customerId: Long?, paid: Long): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var total = 0L
            for (l in lines) total += lineTotal(l.price, l.qty)
            val cv = ContentValues()
            cv.put("ts", System.currentTimeMillis())
            cv.put("total", total)
            cv.put("paid", paid)
            cv.put("method", method)
            if (customerId != null) cv.put("customer_id", customerId) else cv.putNull("customer_id")
            val saleId = db.insertOrThrow("sales", null, cv)
            for (l in lines) {
                val cost = rawProduct(db, l.product.id)?.buy ?: 0L
                val ic = ContentValues()
                ic.put("sale_id", saleId)
                ic.put("product_id", l.product.id)
                ic.put("qty", l.qty)
                ic.put("price", l.price)
                ic.put("cost", cost)
                db.insertOrThrow("sale_items", null, ic)
                db.execSQL(
                    "UPDATE products SET stock = stock - ? WHERE id = ?",
                    arrayOf<Any>(l.qty, l.product.id)
                )
            }
            db.setTransactionSuccessful()
            return saleId
        } finally {
            db.endTransaction()
        }
    }

    fun cancelSale(id: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var already = true
            db.rawQuery("SELECT cancelled FROM sales WHERE id=?", arrayOf(id.toString())).use {
                if (it.moveToFirst()) already = it.getInt(0) == 1
            }
            if (!already) {
                val items = ArrayList<Pair<Long, Double>>()
                db.rawQuery("SELECT product_id, qty FROM sale_items WHERE sale_id=?", arrayOf(id.toString())).use {
                    while (it.moveToNext()) items.add(Pair(it.getLong(0), it.getDouble(1)))
                }
                for ((pid, qty) in items) {
                    db.execSQL("UPDATE products SET stock = stock + ? WHERE id = ?", arrayOf<Any>(qty, pid))
                }
                db.execSQL("UPDATE sales SET cancelled = 1 WHERE id = ?", arrayOf<Any>(id))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun itemsText(db: SQLiteDatabase, saleId: Long): String {
        val parts = ArrayList<String>()
        db.rawQuery(
            "SELECT p.name, si.qty FROM sale_items si JOIN products p ON p.id=si.product_id WHERE si.sale_id=?",
            arrayOf(saleId.toString())
        ).use { while (it.moveToNext()) parts.add(it.getString(0) + " × " + fmtQty(it.getDouble(1))) }
        return parts.joinToString(", ")
    }

    fun sales(limit: Int): List<SaleRow> {
        val out = ArrayList<SaleRow>()
        val db = readableDatabase
        db.rawQuery(
            "SELECT s.id, s.ts, s.total, s.paid, s.method, COALESCE(c.name,''), s.cancelled " +
                "FROM sales s LEFT JOIN customers c ON c.id=s.customer_id " +
                "ORDER BY s.ts DESC, s.id DESC LIMIT $limit", null
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                out.add(
                    SaleRow(
                        id, c.getLong(1), c.getLong(2), c.getLong(3), c.getString(4),
                        c.getString(5), c.getInt(6) == 1, itemsText(db, id)
                    )
                )
            }
        }
        return out
    }

    // ---------------------------------------------------------------- Mijozlar va qarzlar

    fun customers(): List<Customer> {
        val out = ArrayList<Customer>()
        readableDatabase.rawQuery(
            "SELECT c.id, c.name, c.phone, " +
                "COALESCE((SELECT SUM(total-paid) FROM sales WHERE customer_id=c.id AND cancelled=0),0) - " +
                "COALESCE((SELECT SUM(amount) FROM debt_payments WHERE customer_id=c.id),0) " +
                "FROM customers c ORDER BY c.name COLLATE NOCASE", null
        ).use { c ->
            while (c.moveToNext()) out.add(Customer(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)))
        }
        return out
    }

    fun addCustomer(name: String, phone: String): Long {
        val cv = ContentValues()
        cv.put("name", name)
        cv.put("phone", phone)
        return writableDatabase.insertOrThrow("customers", null, cv)
    }

    fun receivePayment(customerId: Long, amount: Long) {
        val cv = ContentValues()
        cv.put("ts", System.currentTimeMillis())
        cv.put("customer_id", customerId)
        cv.put("amount", amount)
        writableDatabase.insertOrThrow("debt_payments", null, cv)
    }

    // ---------------------------------------------------------------- Hisobot

    fun report(since: Long, until: Long = Long.MAX_VALUE): Report {
        val db = readableDatabase
        var count = 0
        var revenue = 0L
        var received = 0L
        db.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(total),0), COALESCE(SUM(paid),0) " +
                "FROM sales WHERE cancelled=0 AND ts>=$since AND ts<$until", null
        ).use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
                revenue = it.getLong(1)
                received = it.getLong(2)
            }
        }
        var profit = 0.0
        db.rawQuery(
            "SELECT COALESCE(SUM((si.price - si.cost) * si.qty),0) FROM sale_items si " +
                "JOIN sales s ON s.id=si.sale_id WHERE s.cancelled=0 AND s.ts>=$since AND ts<$until", null
        ).use { if (it.moveToFirst()) profit = it.getDouble(0) }
        var debtPaid = 0L
        db.rawQuery("SELECT COALESCE(SUM(amount),0) FROM debt_payments WHERE ts>=$since AND ts<$until", null)
            .use { if (it.moveToFirst()) debtPaid = it.getLong(0) }
        var purchases = 0.0
        db.rawQuery(
            "SELECT COALESCE(SUM(price*qty),0) FROM moves WHERE kind='IN' AND ts>=$since AND ts<$until " +
                "AND note NOT IN (?, ?)", arrayOf(INITIAL_NOTE, ADJUST_NOTE)
        ).use { if (it.moveToFirst()) purchases = it.getDouble(0) }
        var writeoff = 0.0
        db.rawQuery(
            "SELECT COALESCE(SUM(price*qty),0) FROM moves WHERE kind='OUT' AND ts>=$since AND ts<$until", null
        ).use { if (it.moveToFirst()) writeoff = it.getDouble(0) }
        val top = ArrayList<Pair<String, Long>>()
        db.rawQuery(
            "SELECT p.name, SUM(si.price*si.qty) AS t FROM sale_items si " +
                "JOIN sales s ON s.id=si.sale_id JOIN products p ON p.id=si.product_id " +
                "WHERE s.cancelled=0 AND s.ts>=$since AND ts<$until GROUP BY p.id ORDER BY t DESC LIMIT 5", null
        ).use { while (it.moveToNext()) top.add(Pair(it.getString(0), Math.round(it.getDouble(1)))) }
        return Report(
            count, revenue, Math.round(profit), received + debtPaid, revenue - received,
            Math.round(purchases), Math.round(writeoff), top
        )
    }

    // ---------------------------------------------------------------- CSV eksport

    fun productsCsv(): String {
        val sb = StringBuilder("﻿")
        sb.append(csvLine("Nomi", "Kod", "Kategoriya", "Birlik", "Olish narxi", "Sotuv narxi", "Qoldiq", "Minimal qoldiq"))
        sb.append("\r\n")
        for (p in products()) {
            sb.append(csvLine(p.name, p.code, p.category, p.unit, p.buy, p.sell, fmtQty(p.stock), fmtQty(p.minStock)))
            sb.append("\r\n")
        }
        return sb.toString()
    }

    fun salesCsv(): String {
        val sb = StringBuilder("﻿")
        sb.append(csvLine("Sana", "Chek raqami", "Mahsulot", "Miqdor", "Narxi", "Summa", "To'lov turi", "Mijoz", "Holati"))
        sb.append("\r\n")
        readableDatabase.rawQuery(
            "SELECT s.ts, s.id, p.name, si.qty, si.price, s.method, COALESCE(c.name,''), s.cancelled " +
                "FROM sale_items si JOIN sales s ON s.id=si.sale_id JOIN products p ON p.id=si.product_id " +
                "LEFT JOIN customers c ON c.id=s.customer_id ORDER BY s.ts, s.id", null
        ).use { c ->
            while (c.moveToNext()) {
                val qty = c.getDouble(3)
                val price = c.getLong(4)
                sb.append(
                    csvLine(
                        fmtDate(c.getLong(0)), c.getLong(1), c.getString(2), fmtQty(qty), price,
                        lineTotal(price, qty), c.getString(5), c.getString(6),
                        if (c.getInt(7) == 1) "Bekor qilingan" else "Amalda"
                    )
                )
                sb.append("\r\n")
            }
        }
        return sb.toString()
    }
}
