# Do'kon Hisobi 2.0

Idish-tovoq va uy-xo'jalik do'koni uchun ombor, sotuv va **tovar jo'natish/qabul qilish** ilovasi (Kotlin + Jetpack Compose).

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
5. Ikkala telefonda ilovani oching: rolni tanlang, **bir xil baza havolasi** va **bir xil do'kon kodi** kiriting (kodni uzun va tasodifiy qiling, u parol vazifasini bajaradi), "Ulanishni tekshirish" ni bosing.

## APK olish
Hamma narsani GitHub'ga yuklang (`main`), Actions → "Build APK" → Artifacts → `Dokon-Hisobi-APK`.
