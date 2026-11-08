## QuickStack — Terraria üslubunda inventar idarəsi

**Z** düyməsini bas və qənimətinin onu artıq saxlayan sandıqlara necə uçduğuna bax. QuickStack Terraria-nın
*yaxın sandıqlara sürətli yığma* funksiyasını Minecraft-a gətirir; bununla yanaşı kilidli yuvalar, sıralama,
yaxın sandıqlardan hazırlama və sandıq otaqlarını rahat edən bir neçə kiçik təkmilləşdirmə də var.

### ✨ Yaxın sandıqlara sürətli yığma
**Z** düyməsini bas (və ya inventardakı düyməni). İnventar bağlanır, əşyaların qarşına atılır və eyni əşyanı
artıq saxlayan yaxın sandıqlara, çəlləklərə və şalker qutularına bir-bir uçur. Əşyalar düşəndə sandıqlar
qapaqlarını açır.

### 🔒 Kilidli yuvalar
Yuvanı kilidləmək üçün **Alt + klik**. Kilidli yuvalar heç vaxt boşalmır: nə sürətli yığmada, nə "Hamısını qoy"da,
nə də sıralamada. Sürətli giriş paneli standart olaraq kilidlidir; zireh və ikinci ələ heç vaxt toxunulmur.

### 📦 Sandıq düymələri
Hər sandıq, çəllək və şalker qutusu bir sıra kiçik düymə alır:
- **Bu sandığa yığ** — yalnız orada artıq olan əşyalar
- **Doldur** — üzərindəki əşyaları sandıqdan götür, hər birindən 1 ədəd saxla (tənzimlənir)
- **Hamısını qoy** / **Hamısını götür**
- Sandığı **sırala**
- Bu sandığı yaxın sandıqlara yığmadan **çıxar**

### 🧹 Sıralama
İnventarını və ya istənilən sandığı düymə, klaviş və ya **orta kliklə** sırala. Kateqoriyaya, ada və ya saya görə.
Natamam dəstələr birləşdirilir.

### 🖱️ Shift ilə sürüşdürmə
**Shift + siçanın sol düyməsini** basılı saxla və yuvaların üzərindən keç: keçdiyin hər yuva Shift + klik kimi
digər inventara köçürülür. Kilidli yuvalar ötürülür.

### 🎡 Siçan təkəri — bir-bir
Dəstənin üzərində **təkəri yuxarı** fırlatmaq daha bir əşya götürür, **aşağı** fırlatmaq birini geri qoyur — sağ
klik kimi, amma daha sürətli.

### 🛠️ Yaxın sandıqlardan hazırlama
Resept kitabı ətrafındakı sandıqlardakı əşyaları da sayır, hazırlama şəbəkəsi isə çatışmayan inqrediyentləri
birbaşa onlardan götürür. İş masasını bağlayanda artıq qalanlar gəldikləri sandığa qayıdır.

### 🔍 Əşya axtarışı
**F6** ətrafındakı sandıqlarda saxlanan hər şeyin şəbəkəsini açır. Süzmək üçün yaz, əşyaya klik et — onu saxlayan
sandıqlar divarların arxasından da görünən qığılcım sütunu ilə parlayır.

### ↩️ Geri al
Səhv şeyi yığdın? *Geri al* düyməsini təyin et və son yığmanı geri qaytar.

---

### ⚙️ Ayarlar
Hər şey oyun içində dəyişdirilə bilər: inventardakı dişli çarx düyməsi, Mod Menu (Fabric) və ya modlar siyahısı
(NeoForge). Tək oyunçu rejimində ikinci vərəq dünyanın limitlərini təyin edir: radius, düşmə sürəti, yaxın
sandıqlardan hazırlama, mod sandıqları.

### 🚀 Performans
Bütün köçürmələri server hər tikdə kiçik hissələrlə edir, uçan əşyalar isə yalnız müştəri tərəfindəki vizual
effektdir, buna görə heç nə itə və ya çoxala bilməz. 16 blok radiusunda **2 352 dolu sandıqla** sınaqdan
keçirilib: planlaşdırma təxminən 7 ms çəkir və 36 dəstə düşərkən heç bir server tiki 5 ms-ni keçmədi.

### 📋 Tələblər
- Minecraft **26.3**
- **Fabric** (Fabric API ilə) və ya **NeoForge**
- **Həm müştəriyə, həm serverə** quraşdırılmalıdır (tək oyunçu rejimində dərhal işləyir)
- İstəyə bağlı: Mod Menu

Vanilla konteynerləri kimi işləyən mod sandıqları (18+ yuva) avtomatik dəstəklənir.
