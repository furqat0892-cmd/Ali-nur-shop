package uz.dokon.hisob

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Kirish: login hamma uchun bitta (ali_nur_sh0p), parol esa har rol uchun alohida
 * ("owner" = Do'kon egasi, "receiver" = Sotuvchi).
 *
 * Parollar telefonda xesh (tuz bilan, 20 000 marta) ko'rinishida saqlanadi, asl parol saqlanmaydi.
 * Parol o'rnatilganda 8 belgili bir martalik TIKLASH KODI beriladi; parol unutilsa shu kod bilan
 * yangi parol o'rnatiladi.
 */
object Auth {
    const val LOGIN = "ali_nur_sh0p"
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private fun toHex(b: ByteArray): String {
        val sb = StringBuilder()
        for (x in b) sb.append(String.format("%02x", x))
        return sb.toString()
    }

    private fun stretch(secret: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val saltBytes = salt.toByteArray(Charsets.UTF_8)
        var h = md.digest((salt + ":" + secret).toByteArray(Charsets.UTF_8))
        for (i in 0 until 20000) {
            md.update(h)
            md.update(saltBytes)
            h = md.digest()
        }
        return toHex(h)
    }

    private fun newSalt(): String {
        val b = ByteArray(12)
        SecureRandom().nextBytes(b)
        return toHex(b)
    }

    private fun pack(secret: String): String {
        val salt = newSalt()
        return salt + ":" + stretch(secret, salt)
    }

    private fun verify(packed: String?, secret: String): Boolean {
        if (packed == null) return false
        val i = packed.indexOf(':')
        if (i <= 0) return false
        return stretch(secret, packed.substring(0, i)) == packed.substring(i + 1)
    }

    private fun other(role: String): String = if (role == "owner") "receiver" else "owner"

    fun roleName(role: String): String = if (role == "owner") "Do'kon egasi" else "Sotuvchi"

    fun isSet(role: String): Boolean = Store.prefs.contains("pw_$role")

    fun check(role: String, pw: String): Boolean = verify(Store.prefs.getString("pw_$role", null), pw)

    /** Egasi va sotuvchi paroli bir xil bo'lib qolmasligi uchun. */
    fun sameAsOther(role: String, pw: String): Boolean =
        isSet(other(role)) && check(other(role), pw)

    /** Telefon raqamidan faqat raqamlar, oxirgi 9 tasi (+998 90 123 45 67 -> 901234567). */
    fun normPhone(s: String): String = s.filter { it.isDigit() }.takeLast(9)

    fun phoneValid(s: String): Boolean = s.filter { it.isDigit() }.length >= 9

    fun hasPhone(role: String): Boolean = Store.prefs.contains("ph_$role")

    fun checkPhone(role: String, phone: String): Boolean =
        Store.prefs.getString("ph_$role", null) == normPhone(phone)

    fun savePhone(role: String, phone: String) {
        Store.prefs.edit().putString("ph_$role", normPhone(phone)).apply()
    }

    private fun normCode(s: String): String = s.uppercase().filter { it.isLetterOrDigit() }

    fun checkRecovery(role: String, code: String): Boolean =
        verify(Store.prefs.getString("rc_$role", null), normCode(code))

    private fun newCode(): String {
        val r = SecureRandom()
        val sb = StringBuilder()
        for (i in 0 until 8) sb.append(ALPHABET[r.nextInt(ALPHABET.length)])
        return sb.toString()
    }

    /**
     * Parolni saqlaydi (birinchi o'rnatish yoki tiklash) va yangi tiklash kodini
     * "XXXX-XXXX" ko'rinishida qaytaradi. Kod faqat shu paytda ko'rsatiladi.
     */
    fun setPassword(role: String, pw: String): String {
        val code = newCode()
        Store.prefs.edit()
            .putString("pw_$role", pack(pw))
            .putString("rc_$role", pack(code))
            .apply()
        return code.substring(0, 4) + "-" + code.substring(4)
    }
}
