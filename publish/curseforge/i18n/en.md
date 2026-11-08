## QuickStack — Terraria-style inventory management

Press **Z** and watch your loot fly into the chests that already hold it. QuickStack brings Terraria's
*quick stack to nearby chests* to Minecraft, together with locked slots, sorting, crafting from nearby chests
and a handful of small tweaks that make chest rooms painless.

### ✨ Quick stack to nearby chests
Press **Z** (or the button in your inventory). The inventory closes, your items burst out in front of you and
fly one by one into the nearby chests, barrels and shulker boxes that already contain the same item. Chests open
their lids as the items land.

![Quick stack](GIF:quick_stack)

### 🔒 Locked slots
**Alt + click** a slot to lock it. Locked slots never leave: not by quick stack, not by "deposit all", not by
sorting. The hotbar is locked by default; armour and offhand are never touched.

![Locked slots](GIF:lock_slots)

### 📦 Chest buttons
Every chest, barrel and shulker box gets a row of small buttons:
- **Quick stack into this chest** — only items it already holds
- **Restock** — take the items you carry from the chest, leaving 1 of each behind (configurable)
- **Deposit all** / **Loot all**
- **Sort** the chest
- **Exclude** this chest from quick stack to nearby

![Chest buttons](GIF:chest_buttons)

### 🧹 Sorting
Sort your inventory or any chest with a button, a key or a **middle click**. By category, name or count.
Partial stacks are merged.

![Sorting](GIF:sort)

### 🖱️ Shift sweep
Hold **Shift + left mouse button** and sweep over slots: every slot you pass is shift-clicked into the other
inventory. Locked slots are skipped.

![Shift sweep](GIF:shift_sweep)

### 🎡 Mouse wheel, one item at a time
**Wheel up** over a stack picks up one more item, **wheel down** puts one back — like right clicks, but fast.

![Mouse wheel](GIF:wheel)

### 🛠️ Crafting from nearby chests
The recipe book counts the items in chests around you and the crafting grid takes missing ingredients straight
from them. Leftovers go back to the chest they came from when you close the table.

![Crafting from nearby chests](GIF:crafting)

### 🔍 Find items
**F6** opens a grid of everything stored in the chests around you. Type to filter, click an item, and the chests
holding it light up with a column of sparkles you can see through walls.

![Find items](GIF:search)

### ↩️ Undo
Stacked the wrong thing? Bind the *Undo* key and take the last quick stack back.

---

### ⚙️ Settings
Everything can be changed in game: the gear button in the inventory, Mod Menu (Fabric) or the Mods list
(NeoForge). In singleplayer the second tab sets the world limits: radius, landing speed, crafting from nearby,
modded chests.

### 🚀 Performance
All transfers are done by the server in small batches per tick, and the flying items are client-side visuals
only, so nothing can be lost or duplicated. Tested with **2 352 full chests** within a 16-block radius: planning
takes about 7 ms and no server tick exceeded 5 ms while 36 stacks landed.

### 📋 Requirements
- Minecraft **26.3**
- **Fabric** (with Fabric API) or **NeoForge**
- Install on **both client and server** (singleplayer works out of the box)
- Optional: Mod Menu

Modded chests that work like vanilla containers (18+ slots) are supported automatically.

### 🌍 Languages
English, Українська, Deutsch, Français, Español, Português (Brasil), Italiano, Polski, Čeština, Türkçe,
Nederlands, 日本語, 한국어, 简体中文, 繁體中文, Қазақша, Oʻzbekcha, Кыргызча, Azərbaycanca, Հայերեն, ქართული,
Lietuvių, Latviešu, Eesti, Română, Монгол.
