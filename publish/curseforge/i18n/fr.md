## QuickStack — gestion d'inventaire façon Terraria

Appuyez sur **Z** et regardez votre butin s'envoler vers les coffres qui en contiennent déjà. QuickStack apporte à
Minecraft le *rangement rapide dans les coffres proches* de Terraria, avec des emplacements verrouillés, le tri, la
fabrication depuis les coffres proches et une poignée de petites améliorations qui rendent les salles de stockage
indolores.

### ✨ Rangement rapide dans les coffres proches
Appuyez sur **Z** (ou sur le bouton de l'inventaire). L'inventaire se ferme, vos objets jaillissent devant vous et
volent un par un vers les coffres, tonneaux et boîtes de Shulker proches qui contiennent déjà le même objet. Les
coffres ouvrent leur couvercle quand les objets y atterrissent.

### 🔒 Emplacements verrouillés
**Alt + clic** sur un emplacement pour le verrouiller. Les emplacements verrouillés ne bougent jamais : ni avec le
rangement rapide, ni avec « Tout déposer », ni avec le tri. La barre d'action est verrouillée par défaut ; l'armure
et la seconde main ne sont jamais touchées.

### 📦 Boutons des coffres
Chaque coffre, tonneau et boîte de Shulker reçoit une rangée de petits boutons :
- **Ranger dans ce coffre** — seulement les objets qu'il contient déjà
- **Se réapprovisionner** — reprendre au coffre les objets que vous portez, en y laissant 1 de chaque (réglable)
- **Tout déposer** / **Tout prendre**
- **Trier** le coffre
- **Exclure** ce coffre du rangement dans les coffres proches

### 🧹 Tri
Triez votre inventaire ou n'importe quel coffre avec un bouton, une touche ou un **clic molette**. Par catégorie,
nom ou quantité. Les piles incomplètes sont fusionnées.

### 🖱️ Balayage Shift
Maintenez **Shift + clic gauche** et balayez les emplacements : chaque emplacement survolé est transféré dans l'autre
inventaire comme avec un Shift-clic. Les emplacements verrouillés sont ignorés.

### 🎡 Molette de la souris, un objet à la fois
**Molette vers le haut** sur une pile prend un objet de plus, **molette vers le bas** en repose un — comme des clics
droits, mais en plus rapide.

### 🛠️ Fabrication depuis les coffres proches
Le livre de recettes compte les objets des coffres autour de vous, et la grille de fabrication y prend directement
les ingrédients manquants. Les restes retournent dans leur coffre d'origine quand vous fermez l'établi.

### 🔍 Trouver des objets
**F6** ouvre une grille de tout ce qui est stocké dans les coffres autour de vous. Tapez pour filtrer, cliquez sur un
objet, et les coffres qui le contiennent s'illuminent d'une colonne d'étincelles visible à travers les murs.

### ↩️ Annuler
Vous avez rangé la mauvaise chose ? Assignez la touche *Annuler* et récupérez le dernier rangement.

---

### ⚙️ Options
Tout se règle en jeu : le bouton en forme d'engrenage dans l'inventaire, Mod Menu (Fabric) ou la liste des mods
(NeoForge). En solo, le second onglet fixe les limites du monde : rayon, vitesse d'atterrissage, fabrication depuis
les coffres proches, coffres moddés.

### 🚀 Performances
Tous les transferts sont faits par le serveur en petits lots à chaque tick, et les objets volants ne sont qu'un effet
visuel côté client : rien ne peut être perdu ni dupliqué. Testé avec **2 352 coffres pleins** dans un rayon de
16 blocs : la planification prend environ 7 ms et aucun tick serveur n'a dépassé 5 ms pendant l'atterrissage de
36 piles.

### 📋 Prérequis
- Minecraft **26.3**
- **Fabric** (avec Fabric API) ou **NeoForge**
- À installer **sur le client et sur le serveur** (en solo, tout fonctionne directement)
- Optionnel : Mod Menu

Les coffres moddés qui fonctionnent comme les conteneurs vanilla (18+ emplacements) sont pris en charge automatiquement.
