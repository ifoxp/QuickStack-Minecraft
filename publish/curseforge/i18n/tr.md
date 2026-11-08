## QuickStack — Terraria tarzı envanter yönetimi

**Z**'ye bas ve ganimetinin, onu zaten içeren sandıklara uçuşunu izle. QuickStack, Terraria'nın *yakındaki
sandıklara hızlı yerleştirme* özelliğini Minecraft'a getiriyor; yanında kilitli yuvalar, sıralama, yakındaki
sandıklardan üretim ve sandık odalarını dert olmaktan çıkaran bir avuç küçük iyileştirme de var.

### ✨ Yakındaki sandıklara hızlı yerleştirme
**Z**'ye bas (ya da envanterdeki düğmeye tıkla). Envanter kapanır, eşyaların önüne fırlar ve aynı eşyayı zaten
içeren yakındaki sandıklara, varillere ve Shulker kutularına tek tek uçar. Eşyalar indikçe sandıkların kapakları
açılır.

### 🔒 Kilitli yuvalar
Bir yuvayı kilitlemek için **Alt + tıkla**. Kilitli yuvalar asla boşalmaz: ne hızlı yerleştirmede, ne "Hepsini
koy"da, ne de sıralamada. Hızlı erişim çubuğu varsayılan olarak kilitlidir; zırha ve ikincil ele hiç dokunulmaz.

### 📦 Sandık düğmeleri
Her sandık, varil ve Shulker kutusu bir sıra küçük düğme kazanır:
- **Bu sandığa yerleştir** — sadece içinde zaten olan eşyalar
- **İkmal yap** — üzerindeki eşyalardan sandıkta olanları al, her birinden 1 tane bırak (ayarlanabilir)
- **Hepsini koy** / **Hepsini al**
- Sandığı **sırala**
- Bu sandığı yakındaki sandıklara yerleştirmeden **hariç tut**

### 🧹 Sıralama
Envanterini ya da herhangi bir sandığı bir düğmeyle, bir tuşla veya **orta tıkla** sırala. Kategoriye, ada ya da
miktara göre. Yarım yığınlar birleştirilir.

### 🖱️ Shift ile sürükleme
**Shift + sol fare tuşunu** basılı tut ve yuvaların üzerinden geç: üzerinden geçtiğin her yuva, Shift + tıklama
gibi diğer envantere aktarılır. Kilitli yuvalar atlanır.

### 🎡 Fare tekerleği, birer birer
Bir yığının üzerinde **tekerleği yukarı** çevirmek bir eşya daha alır, **aşağı** çevirmek birini geri koyar — sağ
tıklama gibi, ama çok daha hızlı.

### 🛠️ Yakındaki sandıklardan üretim
Tarif kitabı etrafındaki sandıklardaki eşyaları da sayar ve üretim ızgarası eksik malzemeleri doğrudan onlardan
alır. Çalışma masasını kapattığında artanlar geldikleri sandığa geri döner.

### 🔍 Eşya bulma
**F6**, etrafındaki sandıklarda saklanan her şeyi gösteren bir ızgara açar. Filtrelemek için yaz, bir eşyaya tıkla;
onu içeren sandıklar duvarların arkasından bile görünen bir kıvılcım sütunuyla parlar.

### ↩️ Geri al
Yanlış şeyi mi yerleştirdin? *Geri al* tuşunu ata ve son hızlı yerleştirmeyi geri getir.

---

### ⚙️ Ayarlar
Her şey oyun içinden değiştirilebilir: envanterdeki dişli düğmesi, Mod Menu (Fabric) ya da mod listesi (NeoForge).
Tek oyunculu modda ikinci sekme dünya sınırlarını belirler: yarıçap, iniş hızı, yakındaki sandıklardan üretim, mod
sandıkları.

### 🚀 Performans
Tüm aktarımları sunucu tik başına küçük partiler halinde yapar; uçan eşyalar ise yalnızca istemci tarafındaki görsel
efektlerdir, bu yüzden hiçbir şey kaybolamaz ya da kopyalanamaz. 16 blok yarıçap içinde **2 352 dolu sandıkla** test
edildi: planlama yaklaşık 7 ms sürüyor ve 36 yığın inerken hiçbir sunucu tiki 5 ms'yi aşmadı.

### 📋 Gereksinimler
- Minecraft **26.3**
- **Fabric** (Fabric API ile) ya da **NeoForge**
- **Hem istemciye hem sunucuya** kurulmalı (tek oyunculu modda doğrudan çalışır)
- İsteğe bağlı: Mod Menu

Vanilla kaplar gibi çalışan mod sandıkları (18+ yuva) otomatik olarak desteklenir.
