## QuickStack — gestione dell'inventario in stile Terraria

Premi **Z** e guarda il tuo bottino volare nei bauli che lo contengono già. QuickStack porta in Minecraft il
*deposito rapido nei bauli vicini* di Terraria, insieme a slot bloccati, ordinamento, fabbricazione dai bauli vicini e
una manciata di piccole migliorie che rendono indolori le stanze dei bauli.

### ✨ Deposito rapido nei bauli vicini
Premi **Z** (o il pulsante nell'inventario). L'inventario si chiude, i tuoi oggetti schizzano fuori davanti a te e
volano uno alla volta nei bauli, barili e scatole di shulker vicini che contengono già lo stesso oggetto. I bauli
aprono il coperchio quando gli oggetti atterrano.

### 🔒 Slot bloccati
**Alt + clic** su uno slot per bloccarlo. Gli slot bloccati non si svuotano mai: né col deposito rapido, né con
"Deposita tutto", né con l'ordinamento. La barra rapida è bloccata di default; armatura e mano secondaria non vengono
mai toccate.

### 📦 Pulsanti dei bauli
Ogni baule, barile e scatola di shulker riceve una fila di piccoli pulsanti:
- **Deposita in questo baule** — solo gli oggetti che contiene già
- **Rifornisciti** — prendi dal baule gli oggetti che hai con te, lasciandone 1 per tipo (configurabile)
- **Deposita tutto** / **Prendi tutto**
- **Ordina** il baule
- **Escludi** questo baule dal deposito nei bauli vicini

### 🧹 Ordinamento
Ordina l'inventario o qualsiasi baule con un pulsante, un tasto o un **clic centrale**. Per categoria, nome o
quantità. Le pile incomplete vengono unite.

### 🖱️ Trascina con Shift
Tieni premuti **Shift + tasto sinistro del mouse** e passa sopra gli slot: ogni slot su cui passi viene spostato
nell'altro inventario come con Shift + clic. Gli slot bloccati vengono saltati.

### 🎡 Rotella del mouse, un oggetto alla volta
**Rotella su** sopra una pila prende un oggetto in più, **rotella giù** ne rimette uno — come i clic destri, ma più
veloce.

### 🛠️ Fabbricazione dai bauli vicini
Il ricettario conta gli oggetti nei bauli intorno a te e la griglia di fabbricazione prende da lì gli ingredienti
mancanti. Gli avanzi tornano nel baule da cui provenivano quando chiudi il banco da lavoro.

### 🔍 Trova oggetti
**F6** apre una griglia con tutto ciò che è conservato nei bauli intorno a te. Scrivi per filtrare, clicca su un
oggetto e i bauli che lo contengono si illuminano con una colonna di scintille visibile attraverso i muri.

### ↩️ Annulla
Hai depositato la cosa sbagliata? Assegna il tasto *Annulla* e riprenditi l'ultimo deposito.

---

### ⚙️ Impostazioni
Tutto si può cambiare in gioco: dal pulsante a ingranaggio nell'inventario, da Mod Menu (Fabric) o dall'elenco delle
mod (NeoForge). In giocatore singolo, la seconda scheda imposta i limiti del mondo: raggio, velocità di atterraggio,
fabbricazione dai bauli vicini, bauli delle mod.

### 🚀 Prestazioni
Tutti i trasferimenti li esegue il server a piccoli lotti per tick, e gli oggetti in volo sono solo un effetto
grafico lato client: niente può andare perso o duplicarsi. Testato con **2 352 bauli pieni** in un raggio di
16 blocchi: la pianificazione richiede circa 7 ms e nessun tick del server ha superato i 5 ms mentre atterravano
36 pile.

### 📋 Requisiti
- Minecraft **26.3**
- **Fabric** (con Fabric API) o **NeoForge**
- Da installare **sia sul client sia sul server** (in giocatore singolo funziona subito)
- Facoltativo: Mod Menu

I bauli delle mod che funzionano come i contenitori vanilla (18+ slot) sono supportati automaticamente.
