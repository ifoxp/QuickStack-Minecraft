## QuickStack — gestionarea inventarului în stilul Terraria

Apasă **Z** și privește cum prada ta zboară în cuferele care o conțin deja. QuickStack aduce în Minecraft
*depozitarea rapidă în cuferele din jur* din Terraria, împreună cu sloturi blocate, sortare, fabricare din cuferele
din jur și câteva mici îmbunătățiri care fac camerele cu cufere mult mai ușor de gestionat.

### ✨ Depozitare rapidă în cuferele din jur
Apasă **Z** (sau butonul din inventar). Inventarul se închide, obiectele țâșnesc în fața ta și zboară unul câte unul
în cuferele, butoaiele și cutiile Shulker din jur care conțin deja același obiect. Cuferele își deschid capacul
când obiectele aterizează.

### 🔒 Sloturi blocate
**Alt + clic** pe un slot îl blochează. Sloturile blocate nu se golesc niciodată: nici la depozitarea rapidă, nici
la „Pune tot”, nici la sortare. Bara rapidă este blocată implicit; armura și mâna secundară nu sunt atinse
niciodată.

### 📦 Butoanele cufărului
Fiecare cufăr, butoi și cutie Shulker primește un rând de butoane mici:
- **Depozitează în acest cufăr** — doar obiectele pe care le conține deja
- **Reaprovizionează** — ia din cufăr obiectele pe care le porți, lăsând câte 1 din fiecare (configurabil)
- **Pune tot** / **Ia tot**
- **Sortează** cufărul
- **Exclude** acest cufăr de la depozitarea în cuferele din jur

### 🧹 Sortare
Sortează inventarul sau orice cufăr cu un buton, o tastă sau un **clic pe rotiță**. După categorie, nume sau
cantitate. Stivele incomplete sunt unite.

### 🖱️ Glisare cu Shift
Ține apăsat **Shift + butonul stâng al mouse-ului** și trece peste sloturi: fiecare slot peste care treci e mutat în
celălalt inventar ca la Shift + clic. Sloturile blocate sunt sărite.

### 🎡 Rotița mouse-ului, câte un obiect
**Rotița în sus** deasupra unei stive mai ia un obiect, **rotița în jos** pune unul înapoi — ca clicurile dreapta,
dar mult mai rapid.

### 🛠️ Fabricare din cuferele din jur
Cartea de rețete numără obiectele din cuferele din jurul tău, iar grila de fabricare ia ingredientele lipsă direct
din ele. Resturile se întorc în cufărul din care au venit când închizi masa de lucru.

### 🔍 Găsește obiecte
**F6** deschide o grilă cu tot ce e depozitat în cuferele din jurul tău. Scrie pentru a filtra, dă clic pe un obiect,
iar cuferele care îl conțin se aprind cu o coloană de scântei vizibilă prin pereți.

### ↩️ Anulare
Ai depozitat ce nu trebuia? Atribuie tasta *Anulare* și recuperează ultima depozitare.

---

### ⚙️ Setări
Totul se poate schimba în joc: din butonul cu rotiță dințată din inventar, din Mod Menu (Fabric) sau din lista de
moduri (NeoForge). În modul un jucător, a doua filă stabilește limitele lumii: rază, viteză de aterizare, fabricare
din cuferele din jur, cufere din moduri.

### 🚀 Performanță
Toate transferurile sunt făcute de server în loturi mici pe tick, iar obiectele care zboară sunt doar efecte vizuale
pe client, așa că nimic nu se poate pierde sau duplica. Testat cu **2 352 de cufere pline** pe o rază de 16 blocuri:
planificarea durează circa 7 ms și niciun tick de server nu a depășit 5 ms în timp ce aterizau 36 de stive.

### 📋 Cerințe
- Minecraft **26.3**
- **Fabric** (cu Fabric API) sau **NeoForge**
- Se instalează **atât pe client, cât și pe server** (în modul un jucător merge direct)
- Opțional: Mod Menu

Cuferele din moduri care funcționează ca recipientele vanilla (18+ sloturi) sunt suportate automat.
