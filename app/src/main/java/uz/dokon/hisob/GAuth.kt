package uz.dokon.hisob

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Gmail (email) + parol bilan kirish: Firebase Authentication REST API.
 * Parol unutilsa Gmail'ga tiklash havolasi yuboriladi.
 */
object GAuth {
    /** Firebase konsoli -> Project settings -> General -> Web API Key */
    const val API_KEY = "AIzaSyC3RzSgyCTjpGDMjRSo0gDd1nbHDLw8F9c"

    private fun friendly(msg: String): String = when {
        msg.contains("EMAIL_EXISTS") -> "Bu Gmail allaqachon ro'yxatdan o'tgan. \"Kirish\" ni bosing yoki parolni tiklang"
        msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("INVALID_PASSWORD") ||
            msg.contains("EMAIL_NOT_FOUND") -> "Gmail yoki parol noto'g'ri"
        msg.contains("WEAK_PASSWORD") -> "Parol kamida 6 belgidan iborat bo'lsin"
        msg.contains("INVALID_EMAIL") -> "Gmail manzili noto'g'ri yozilgan"
        msg.contains("TOO_MANY_ATTEMPTS") -> "Juda ko'p urinish bo'ldi. Birozdan keyin qayta urinib ko'ring"
        msg.contains("OPERATION_NOT_ALLOWED") -> "Firebase'da Email/Password yoqilmagan (Authentication -> Sign-in method)"
        msg.contains("API_KEY") || msg.contains("API key") -> "Firebase API kaliti noto'g'ri"
        msg.contains("USER_DISABLED") -> "Bu hisob o'chirib qo'yilgan"
        else -> msg
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val c = URL("https://identitytoolkit.googleapis.com/v1/$path?key=$API_KEY").openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = 10000
            c.readTimeout = 15000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
            if (code !in 200..299) {
                val m = json.optJSONObject("error")?.optString("message") ?: "Server xatosi $code"
                throw Exception(friendly(m))
            }
            return json
        } finally {
            c.disconnect()
        }
    }

    private fun creds(email: String, pw: String) = JSONObject()
        .put("email", email).put("password", pw).put("returnSecureToken", true)

    private fun sendVerify(idToken: String) {
        post("accounts:sendOobCode", JSONObject().put("requestType", "VERIFY_EMAIL").put("idToken", idToken))
    }

    /** Yangi hisob ochadi va Gmail'ga tasdiqlash havolasini yuboradi. */
    suspend fun signUp(email: String, pw: String) = withContext(Dispatchers.IO) {
        val r = post("accounts:signUp", creds(email, pw))
        sendVerify(r.getString("idToken"))
    }

    /** Kiradi. Gmail tasdiqlanmagan bo'lsa, havolani qayta yuboradi va xato beradi. */
    suspend fun signIn(email: String, pw: String) = withContext(Dispatchers.IO) {
        val r = post("accounts:signInWithPassword", creds(email, pw))
        val token = r.getString("idToken")
        val info = post("accounts:lookup", JSONObject().put("idToken", token))
        val verified = info.optJSONArray("users")?.optJSONObject(0)?.optBoolean("emailVerified", false) ?: false
        if (!verified) {
            try { sendVerify(token) } catch (_: Exception) {}
            throw Exception("Gmail hali tasdiqlanmagan. Pochtangizga (Spam ham) havola yubordik. Uni bosib, qayta kiring")
        }
        Unit
    }

    /** Parolni tiklash havolasini Gmail'ga yuboradi. */
    suspend fun resetPassword(email: String) = withContext(Dispatchers.IO) {
        post("accounts:sendOobCode", JSONObject().put("requestType", "PASSWORD_RESET").put("email", email))
        Unit
    }
}
