# Do'kon Hisobi

Idish-tovoq va uy-xo'jalik do'koni uchun ombor va sotuv hisobi Android ilovasi (Kotlin + Jetpack Compose, SQLite). Internet shart emas, hamma ma'lumot telefonning o'zida saqlanadi.

## Imkoniyatlar
- **Sotuv**: nomi yoki kodi bo'yicha qidirish, savat, narx/miqdorni o'zgartirish, Naqd / Karta / Nasiya, omborda yetmasa ogohlantirish
- **Ombor**: mahsulot qo'shish/tahrirlash/o'chirish, kategoriya, birlik (dona, kg, litr...), minimal qoldiq, "kam qolganlar" filtri, ombor qiymati
- **Kirim**: yetkazib beruvchidan tovar kelishi, olish va sotish narxi, ustama foizi
- **Chiqim**: singan/yaroqsiz tovarni hisobdan chiqarish
- **Tarix**: kirim/chiqim yozuvlarini bekor qilish (qoldiq avtomatik tiklanadi)
- **Hisobot**: bugun / 7 kun / 30 kun: savdo, foyda, cheklar, nasiya, eng ko'p sotilganlar
- **Mijozlar va qarzlar**: nasiya savdo, qarz to'lovlarini qabul qilish
- **Sotuvlar tarixi**: sotuvni bekor qilish (tovar omborga qaytadi)
- **Zaxira nusxa** va tiklash, **CSV eksport** (Excel uchun)

## APK olish
1. Bu papkadagi hamma narsani GitHub repozitoriyga yuklang (`main` shoxiga).
2. GitHub'da **Actions** bo'limiga kiring, "Build APK" tugagach (3-6 daqiqa) pastdagi **Artifacts** dan `Dokon-Hisobi-APK` ni yuklab oling.
3. Zipni ochib, `app-debug.apk` ni telefonga o'rnating.

Imzo kaliti (`app/shop.jks`) repoda turadi, shuning uchun keyingi versiyalar eskisining ustiga ma'lumotlarni o'chirmasdan o'rnatiladi.
