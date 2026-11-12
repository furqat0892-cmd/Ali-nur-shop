# Do'kon Hisobi 2.0

Idish-tovoq va uy-xo'jalik do'koni uchun ombor, sotuv va **tovar jo'natish/qabul qilish** ilovasi (Kotlin + Jetpack Compose).

## Kirish (Gmail va parol)
Ilova ochilganda tepada Ali nur shop brend rasmi, pastda ketma-ket: 1) **Do'kon egasi**, 2) **Sotuvchi**, 3) **Gmail**, 4) **Parol**.
- Birinchi marta: "Ro'yxatdan o'tish" → Gmail va parol (kamida 6 belgi). Gmail'ga tasdiqlash havolasi keladi, uni bosgach kirish mumkin.
- Parol unutilsa: "Gmail'ga havola yuborish" — pochtaga tiklash havolasi keladi.
- Sozlamalar → "Parolni almashtirish" ham Gmail'ga havola yuboradi.
- Firebase'da Authentication → Email/Password yoqilgan bo'lishi va `GAuth.kt` ichida Web API Key yozilgan bo'lishi kerak.
- "Meni eslab qol" belgilangan bo'lsa, keyingi safar kirish ekrani o'tkazib yuboriladi. Chiqish: Sozlamalar → "Chiqish".

## Chat (Telegram uslubida)
Birinchi tabda egasi va sotuvchi o'rtasida chat: matn, rasm (galereyadan), vaqt va ✓ / ✓✓ (o'qildi) belgilari. Tovar jo'natmalari ham chatda pufakcha bo'lib chiqadi, sotuvchi shu yerdan "Qabul qilish"ni bosadi. Ma'lumot Firebase'da: `shops/<kod>/chat`, rasmlar `chat_img`, o'qilgan vaqt `read`.

## Ikki rol (bitta APK)
**Do'kon egasi**: tovarni (nomi, soni, narxi) jo'natadi, qabul natijasini ko'radi, 10 kunlik jo'natma hisobotini ko'radi.

**Qabul qiluvchi (do'kon)**: jo'natilgan tovar "Xabarlar" bo'limida xabar bo'lib keladi. Qabul qilishda har bir tovar uchun singan / yo'q sonini kiritadi. Hammasi joyida bo'lsa "Qabul qildim", kamchilik bo'lsa "Qabul qildim + kamchiliklarni yuborish". Butun tovar avtomatik omborga kirim bo'ladi. Sotuv, ombor, kirim/chiqim, nasiya, zaxira nusxa ham shu rolda.

**10 kunlik hisobot**: 1–10, 11–20, 21–oy oxiri davrlari bo'yicha kirim, chiqim, savdo, sof foyda, kamchilik qiymati (Hisobot → 10 kunlik).

## Firebase sozlash (bir marta, 5 daqiqa)
1. console.firebase.google.com → **Create a project** (nomi ixtiyoriy, Analytics kerak emas).
2. Chapdagi menyu: **Build → Realtime Database → Create Database** → joylashuv tanlang → **Start in test mode**.
3. **Rules** bo'limiga quyidagini qo'ying va **Publish** bosing:
```
{
  "rules": {
    ".read": false,
    ".write": false,
    "shops": { "$code": { ".read": true, ".write": true } }
  }
}
```
4. **Data** bo'limi tepasidagi havolani nusxalang (`https://...firebaseio.com` yoki `https://...firebasedatabase.app`).
5. Ikkala telefonda kiring (rol kirish ekranida tanlanadi), Sozlamalar bo'limiga **bir xil baza havolasi** va **bir xil do'kon kodi** kiriting (kodni uzun va tasodifiy qiling, u parol vazifasini bajaradi), "Ulanishni tekshirish" ni bosing.

## APK olish
Hamma narsani GitHub'ga yuklang (`main`), Actions → "Build APK" → Artifacts → `Dokon-Hisobi-APK`.
