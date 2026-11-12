# Do'kon Hisobi 2.0

Idish-tovoq va uy-xo'jalik do'koni uchun ombor, sotuv va **tovar jo'natish/qabul qilish** ilovasi (Kotlin + Jetpack Compose).

## Kirish (login va parol)
Ilova ochilganda tepada Ali nur shop brend rasmi, pastda ketma-ket: 1) **Do'kon egasi**, 2) **Sotuvchi**, 3) **Login**, 4) **Telefon raqami**, 5) **Parol**.
- Login ikkala rol uchun bir xil: `ali_nur_sh0p`
- Telefon raqami parol o'rnatilganda saqlanadi; keyingi kirishda va parolni tiklashda shu raqam so'raladi.
- Parol har rol uchun **alohida**. Birinchi kirishda rolni tanlab, yangi parol o'rnatiladi (kamida 4 belgi). Egasi va sotuvchi paroli bir xil bo'lishi mumkin emas.
- Parol o'rnatilganda **tiklash kodi** (XXXX-XXXX) beriladi va faqat bir marta ko'rsatiladi. Uni qog'ozga yozib qo'ying.
- Parolni unutsangiz: parol ostidagi **"Parolni unutdingizmi? Tiklash"** ni bosing, tiklash kodini kiriting va yangi parol o'rnating (yangi tiklash kodi beriladi).
- "Meni eslab qol" belgilangan bo'lsa, keyingi safar kirish ekrani o'tkazib yuboriladi. Chiqish: Sozlamalar → "Chiqish".
- Parollar telefonda xesh ko'rinishida saqlanadi, asl parol saqlanmaydi.

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
