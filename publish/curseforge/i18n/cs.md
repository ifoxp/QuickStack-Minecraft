## QuickStack — správa inventáře ve stylu Terrarie

Stiskni **Z** a sleduj, jak tvoje kořist letí do truhel, které ji už obsahují. QuickStack přináší do Minecraftu
*rychlé ukládání do truhel poblíž* z Terrarie a k tomu zamčené sloty, řazení, výrobu z truhel poblíž a hrst drobných
vylepšení, se kterými přestanou skladiště bolet.

### ✨ Rychlé ukládání do truhel poblíž
Stiskni **Z** (nebo tlačítko v inventáři). Inventář se zavře, předměty vyletí před tebe a jeden po druhém letí do
truhel, sudů a shulkerových krabic poblíž, které už stejný předmět obsahují. Truhly při dopadu předmětů otevírají
víka.

### 🔒 Zamčené sloty
**Alt + kliknutím** na slot ho zamkneš. Zamčené sloty se nikdy nevyprázdní: ani při rychlém ukládání, ani při
„Uložit vše“, ani při řazení. Lišta rychlého přístupu je ve výchozím stavu zamčená; brnění a druhé ruky se mod nikdy
nedotkne.

### 📦 Tlačítka truhel
Každá truhla, sud i shulkerová krabice dostane řadu malých tlačítek:
- **Uložit do této truhly** — jen předměty, které už obsahuje
- **Doplnit** — vezmi z truhly předměty, které neseš, a nech od každého 1 (nastavitelné)
- **Uložit vše** / **Vybrat vše**
- **Seřadit** truhlu
- **Vyřadit** tuto truhlu z ukládání do truhel poblíž

### 🧹 Řazení
Seřaď inventář nebo jakoukoli truhlu tlačítkem, klávesou nebo **prostředním tlačítkem myši**. Podle kategorie,
názvu nebo počtu. Neúplné balíky se sloučí.

### 🖱️ Tažení se Shiftem
Drž **Shift + levé tlačítko myši** a přejeď přes sloty: každý slot, přes který přejedeš, se přesune do druhého
inventáře jako při Shift + kliknutí. Zamčené sloty se přeskočí.

### 🎡 Kolečko myši, po jednom předmětu
**Kolečko nahoru** nad balíkem vezme další předmět, **kolečko dolů** jeden vrátí — jako kliknutí pravým tlačítkem,
jen rychleji.

### 🛠️ Výroba z truhel poblíž
Kniha receptů započítá předměty v truhlách kolem tebe a výrobní mřížka si z nich rovnou vezme chybějící suroviny.
Zbytky se po zavření pracovního stolu vrátí do truhly, odkud přišly.

### 🔍 Hledání předmětů
**F6** otevře mřížku se vším, co je uloženo v truhlách kolem tebe. Piš pro filtrování, klikni na předmět a truhly,
které ho obsahují, se rozzáří sloupem jisker viditelným skrz zdi.

### ↩️ Vrácení
Uložil jsi něco špatně? Nastav klávesu *Vrátit* a vezmi poslední ukládání zpět.

---

### ⚙️ Nastavení
Vše se dá změnit ve hře: tlačítkem s ozubeným kolem v inventáři, přes Mod Menu (Fabric) nebo seznam módů (NeoForge).
Ve hře jednoho hráče druhá záložka nastavuje limity světa: dosah, rychlost dopadu, výrobu z truhel poblíž, truhly
z módů.

### 🚀 Výkon
Všechny přesuny provádí server po malých dávkách za tick a létající předměty jsou jen vizuální efekt na straně
klienta, takže se nic nemůže ztratit ani zdvojit. Otestováno na **2 352 plných truhlách** v okruhu 16 bloků:
plánování trvá asi 7 ms a žádný tick serveru nepřekročil 5 ms, zatímco dopadalo 36 balíků.

### 📋 Požadavky
- Minecraft **26.3**
- **Fabric** (s Fabric API) nebo **NeoForge**
- Nainstalovat **na klienta i na server** (ve hře jednoho hráče funguje hned)
- Volitelně: Mod Menu

Truhly z módů, které fungují jako vanilla úložiště (18+ slotů), jsou podporovány automaticky.
