package uz.dokon.hisob

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------ Ma'lumot va bulut

data class ChatMsg(
    val id: String,
    val ts: Long,
    val from: String,
    val name: String,
    val text: String,
    val hasImg: Boolean
)

/** Chat: <baza>/shops/<kod>/chat/<id>.json, rasmlar chat_img/<id>.json, o'qilgan vaqt read/<rol>.json */
object ChatCloud {
    private fun base(): String = Store.dbUrl.trim().trimEnd('/') + "/shops/" + Cloud.cleanCode(Store.shopCode)

    suspend fun list(): List<ChatMsg> = withContext(Dispatchers.IO) {
        val text = Cloud.call("GET", base() + "/chat.json?orderBy=%22%24key%22&limitToLast=200", null).trim()
        val out = ArrayList<ChatMsg>()
        if (text.isNotEmpty() && text != "null") {
            val obj = JSONObject(text)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = obj.optJSONObject(k) ?: continue
                out.add(
                    ChatMsg(
                        k, o.optLong("ts"), o.optString("from"), o.optString("name"),
                        o.optString("text"), o.optBoolean("img", false)
                    )
                )
            }
        }
        out.sortedBy { it.ts }
    }

    suspend fun send(m: ChatMsg, imgB64: String?) {
        withContext(Dispatchers.IO) {
            if (imgB64 != null) {
                Cloud.call("PUT", base() + "/chat_img/" + m.id + ".json", JSONObject.quote(imgB64))
            }
            val o = JSONObject()
            o.put("ts", m.ts)
            o.put("from", m.from)
            o.put("name", m.name)
            o.put("text", m.text)
            o.put("img", m.hasImg)
            Cloud.call("PUT", base() + "/chat/" + m.id + ".json", o.toString())
        }
    }

    suspend fun image(id: String): String? = withContext(Dispatchers.IO) {
        val t = Cloud.call("GET", base() + "/chat_img/" + id + ".json", null).trim()
        if (t.isEmpty() || t == "null") null else (JSONTokener(t).nextValue() as? String)
    }

    suspend fun readOf(role: String): Long = withContext(Dispatchers.IO) {
        Cloud.call("GET", base() + "/read/" + role + ".json", null).trim().toLongOrNull() ?: 0L
    }

    suspend fun markRead(role: String, ts: Long) {
        withContext(Dispatchers.IO) {
            Cloud.call("PUT", base() + "/read/" + role + ".json", ts.toString())
        }
    }
}

/** Rasmni kichraytiradi (eng uzun tomoni 900 px) va JPEG qiladi. */
fun prepareImage(ctx: android.content.Context, uri: Uri): ByteArray? {
    try {
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        val maxSide = maxOf(opts.outWidth, opts.outHeight)
        while (maxSide / sample > 1800) sample *= 2
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = sample
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
            ?: return null
        val scale = 900f / maxOf(bmp.width, bmp.height)
        val small: Bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * scale).toInt().coerceAtLeast(1),
                (bmp.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else bmp
        val orient = try {
            ctx.contentResolver.openInputStream(uri)?.use {
                android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
            } ?: 1
        } catch (_: Exception) {
            1
        }
        val deg = when (orient) {
            6 -> 90f
            3 -> 180f
            8 -> 270f
            else -> 0f
        }
        val finalBmp: Bitmap = if (deg != 0f) {
            val m = Matrix()
            m.postRotate(deg)
            Bitmap.createBitmap(small, 0, 0, small.width, small.height, m, true)
        } else small
        val bos = ByteArrayOutputStream()
        finalBmp.compress(Bitmap.CompressFormat.JPEG, 65, bos)
        return bos.toByteArray()
    } catch (_: Exception) {
        return null
    }
}

// ------------------------------------------------------------------ Yordamchilar

private val ChatBg = Color(0xFFDCE8E4)
private val MineBg = Color(0xFFE1F8D4)

private fun timeOf(ts: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(ts))

private fun dayKey(ts: Long): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ts))

private fun dayLabel(ts: Long): String {
    val now = System.currentTimeMillis()
    return when (dayKey(ts)) {
        dayKey(now) -> "Bugun"
        dayKey(now - 86400000L) -> "Kecha"
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date(ts))
    }
}

private data class TItem(val ts: Long, val msg: ChatMsg?, val ship: Shipment?) {
    val key: String get() = if (msg != null) "m" + msg.id else "s" + (ship?.id ?: ts.toString())
}

/** Yuqori panel (Telegram kabi): avatar, sarlavha, kichik yozuv. */
@Composable
fun HeaderBar(title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    Surface(color = Teal, shadowElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).click3d(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2190", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFD166)),
                contentAlignment = Alignment.Center
            ) {
                Text(title.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF5B3A00), fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(subtitle, color = Color(0xCCFFFFFF), fontSize = 12.sp)
            }
        }
    }
}

// ------------------------------------------------------------------ Chat ekrani

@Composable
fun ChatScreen() {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val me = Store.role
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var receiving by remember { mutableStateOf<Shipment?>(null) }
    var viewImg by remember { mutableStateOf<ImageBitmap?>(null) }
    val listState = rememberLazyListState()

    val chat = Store.chat
    val ships = Store.shipments
    val items = remember(chat, ships) {
        val l = ArrayList<TItem>()
        for (m in chat) l.add(TItem(m.ts, m, null))
        for (s in ships) l.add(TItem(s.ts, null, s))
        l.sortedBy { it.ts }
    }

    // Boshqa tomondan kelgan eng oxirgi hodisa vaqti
    val latestIncoming = remember(items) {
        var best = 0L
        for (t in items) {
            val m = t.msg
            val s = t.ship
            if (m != null && m.from != me && m.ts > best) best = m.ts
            if (s != null && me == "receiver" && s.ts > best) best = s.ts
            if (s != null && me == "owner") {
                val rts = s.receipt?.ts ?: 0L
                if (rts > best) best = rts
            }
        }
        best
    }

    DisposableEffect(Unit) {
        Store.chatOpen = true
        onDispose {
            Store.chatOpen = false
            Store.typing = false
        }
    }

    LaunchedEffect(latestIncoming) {
        Store.markSeen(latestIncoming)
        if (Store.configured()) {
            try {
                ChatCloud.markRead(me, System.currentTimeMillis())
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) listState.scrollToItem(items.lastIndex)
    }

    fun send(txt: String, img: ByteArray?) {
        if (!Store.configured()) {
            error = "Baza sozlanmagan. Sozlamalar bo'limida Firebase havolasi va do'kon kodini kiriting"
            return
        }
        val ts = System.currentTimeMillis()
        val id = String.format(Locale.US, "%013d", ts) + "-" + me.take(1) + (1000..9999).random()
        val m = ChatMsg(id, ts, me, Store.myName.ifBlank { Auth.roleName(me) }, txt.trim(), img != null)
        busy = true
        error = null
        scope.launch {
            try {
                var b64: String? = null
                if (img != null) {
                    b64 = Base64.encodeToString(img, Base64.NO_WRAP)
                    val b = BitmapFactory.decodeByteArray(img, 0, img.size)
                    if (b != null) Store.imgCache[id] = b.asImageBitmap()
                }
                ChatCloud.send(m, b64)
                Store.chat = (Store.chat + m).sortedBy { it.ts }
            } catch (e: Exception) {
                error = "Yuborilmadi: ${e.message}"
                if (img == null) text = txt
            } finally {
                busy = false
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { prepareImage(ctx, uri) }
                if (bytes == null) {
                    error = "Rasmni o'qib bo'lmadi"
                } else {
                    val caption = text
                    text = ""
                    send(caption, bytes)
                }
            }
        }
    }

    val rec = receiving
    if (rec != null) {
        ReceiveScreen(rec, onBack = { receiving = null })
    } else {
        Column(Modifier.fillMaxSize().background(ChatBg)) {
            val sync = Store.syncError
            if (sync != null) {
                Text(
                    "Ulanish xatosi: $sync",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().background(Red).padding(horizontal = 12.dp, vertical = 4.dp)
                )
            } else if (!Store.configured()) {
                Text(
                    "Baza sozlanmagan: Sozlamalar bo'limini to'ldiring",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().background(Red).padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            if (items.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Hali xabar yo'q.\nBirinchi bo'lib yozing.", color = Muted)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(items, key = { _, t -> t.key }) { idx, t ->
                        val prev = items.getOrNull(idx - 1)
                        if (prev == null || dayKey(prev.ts) != dayKey(t.ts)) DayChip(t.ts)
                        val m = t.msg
                        val s = t.ship
                        if (m != null) {
                            MsgBubble(m, m.from == me) { viewImg = it }
                        } else if (s != null) {
                            ShipChatBubble(s, me) { receiving = s }
                        }
                    }
                }
            }
            val e = error
            if (e != null) {
                Text(
                    e,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().background(Red).padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Surface(shadowElevation = 6.dp, color = Color.White) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { picker.launch("image/*") }, enabled = !busy) {
                        Text("📷", fontSize = 22.sp)
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f).onFocusChanged { f -> Store.typing = f.isFocused },
                        placeholder = { Text("Xabar yozing...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        enabled = text.isNotBlank() && !busy,
                        onClick = {
                            val t = text
                            text = ""
                            send(t, null)
                        }
                    ) { Icon(Icons.Filled.Send, contentDescription = "Yuborish", tint = if (text.isNotBlank()) Teal else Muted) }
                }
            }
        }
    }

    val big = viewImg
    if (big != null) {
        Dialog(onDismissRequest = { viewImg = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                Modifier.fillMaxSize().background(Color.Black).clickable { viewImg = null },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = big,
                    contentDescription = "Rasm",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun DayChip(ts: Long) {
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0x4D000000)) {
            Text(dayLabel(ts), color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
        }
    }
}

@Composable
private fun TimeTicks(ts: Long, mine: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(timeOf(ts), fontSize = 11.sp, color = Muted)
        if (mine) {
            val read = ts <= Store.readOther
            Spacer(Modifier.width(3.dp))
            Text(
                if (read) "✓✓" else "✓",
                fontSize = 12.sp,
                color = if (read) Color(0xFF2AABEE) else Muted
            )
        }
    }
}

@Composable
private fun MsgBubble(m: ChatMsg, mine: Boolean, onImage: (ImageBitmap) -> Unit) {
    val shape = if (mine) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = shape,
            color = if (mine) MineBg else Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (!mine) {
                    Text(m.name.ifBlank { "Foydalanuvchi" }, color = Teal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (m.hasImg) ChatImage(m.id, onImage)
                if (m.text.isNotBlank()) Text(m.text, fontSize = 16.sp)
                Box(Modifier.align(Alignment.End)) { TimeTicks(m.ts, mine) }
            }
        }
    }
}

@Composable
private fun ChatImage(id: String, onClick: (ImageBitmap) -> Unit) {
    var bmp by remember(id) { mutableStateOf(Store.imgCache[id]) }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (bmp == null) {
            try {
                val b64 = ChatCloud.image(id)
                if (b64 != null) {
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    val b = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    if (b != null) {
                        val ib = b.asImageBitmap()
                        Store.imgCache[id] = ib
                        bmp = ib
                    } else {
                        failed = true
                    }
                } else {
                    failed = true
                }
            } catch (_: Exception) {
                failed = true
            }
        }
    }
    val b = bmp
    if (b != null) {
        val ratio = (b.width.toFloat() / b.height.toFloat()).coerceIn(0.5f, 2f)
        Image(
            bitmap = b,
            contentDescription = "Rasm",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .width(240.dp)
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick(b) }
        )
    } else {
        Box(Modifier.size(width = 200.dp, height = 120.dp), contentAlignment = Alignment.Center) {
            Text(if (failed) "Rasm yuklanmadi" else "Yuklanmoqda...", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ShipChatBubble(s: Shipment, me: String, onReceive: () -> Unit) {
    val mine = me == "owner"
    val shape = if (mine) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = shape,
            color = if (mine) MineBg else Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Tovar jo'natma", color = Teal, fontWeight = FontWeight.Bold)
                if (s.sender.isNotBlank()) Text(s.sender, color = Muted, fontSize = 12.sp)
                if (s.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(s.note)
                }
                Spacer(Modifier.height(6.dp))
                for (i in s.items) {
                    Column(Modifier.padding(vertical = 3.dp)) {
                        Text(i.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${fmtQty(i.qty)} ${i.unit} × ${fmtMoney(i.price)} = ${fmtMoney(lineTotal(i.price, i.qty))} so'm",
                            fontSize = 12.sp,
                            color = Muted
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text("Jami: ${fmtMoney(s.total)} so'm", fontWeight = FontWeight.Bold)
                if (s.receipt == null) {
                    if (me == "receiver") {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onReceive, modifier = Modifier.fillMaxWidth()) { Text("Qabul qilish") }
                    } else {
                        Text("Kutilmoqda: hali qabul qilinmagan", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                } else {
                    StatusFooter(s)
                }
                Box(Modifier.align(Alignment.End).padding(top = 4.dp)) { TimeTicks(s.ts, false) }
            }
        }
    }
}
