## QuickStack — gestión del inventario al estilo de Terraria

Pulsa **Z** y mira cómo tu botín vuela a los cofres que ya lo contienen. QuickStack trae a Minecraft el *depósito
rápido en cofres cercanos* de Terraria, junto con espacios bloqueados, ordenación, fabricación desde cofres cercanos
y un puñado de pequeñas mejoras que hacen que los almacenes dejen de ser un dolor de cabeza.

### ✨ Depósito rápido en cofres cercanos
Pulsa **Z** (o el botón del inventario). El inventario se cierra, tus objetos salen disparados delante de ti y vuelan
uno a uno a los cofres, barriles y cajas de shulker cercanos que ya contienen ese mismo objeto. Los cofres abren la
tapa cuando los objetos aterrizan.

### 🔒 Espacios bloqueados
**Alt + clic** en un espacio para bloquearlo. Los espacios bloqueados nunca se vacían: ni con el depósito rápido, ni
con «Depositar todo», ni al ordenar. La barra de acceso rápido está bloqueada por defecto; la armadura y la mano
secundaria nunca se tocan.

### 📦 Botones del cofre
Cada cofre, barril y caja de shulker recibe una fila de pequeños botones:
- **Depositar en este cofre** — solo los objetos que ya contiene
- **Reabastecerse** — saca del cofre los objetos que ya llevas, dejando 1 de cada uno (configurable)
- **Depositar todo** / **Sacar todo**
- **Ordenar** el cofre
- **Excluir** este cofre del depósito en cofres cercanos

### 🧹 Ordenar
Ordena tu inventario o cualquier cofre con un botón, una tecla o un **clic central**. Por categoría, nombre o
cantidad. Las pilas incompletas se juntan.

### 🖱️ Barrido con Shift
Mantén **Shift + botón izquierdo del ratón** y pasa por encima de los espacios: cada espacio por el que pases se
mueve al otro inventario como con Shift + clic. Los espacios bloqueados se saltan.

### 🎡 Rueda del ratón, objeto a objeto
**Rueda hacia arriba** sobre una pila coge un objeto más, **rueda hacia abajo** devuelve uno: como los clics
derechos, pero más rápido.

### 🛠️ Fabricar desde cofres cercanos
El libro de recetas cuenta los objetos de los cofres que te rodean y la cuadrícula de fabricación toma de ellos los
ingredientes que falten. Lo que sobra vuelve al cofre de donde salió cuando cierras la mesa de trabajo.

### 🔍 Buscar objetos
**F6** abre una cuadrícula con todo lo que hay guardado en los cofres de tu alrededor. Escribe para filtrar, haz clic
en un objeto y los cofres que lo contienen se iluminan con una columna de destellos que se ve a través de las paredes.

### ↩️ Deshacer
¿Has guardado lo que no era? Asigna la tecla *Deshacer* y recupera el último depósito.

---

### ⚙️ Opciones
Todo se puede cambiar en el juego: con el botón del engranaje del inventario, Mod Menu (Fabric) o la lista de mods
(NeoForge). En un jugador, la segunda pestaña fija los límites del mundo: radio, velocidad de aterrizaje,
fabricación desde cofres cercanos y cofres de mods.

### 🚀 Rendimiento
Todas las transferencias las hace el servidor en pequeños lotes por tick, y los objetos voladores son solo un efecto
visual del cliente, así que nada puede perderse ni duplicarse. Probado con **2 352 cofres llenos** en un radio de
16 bloques: la planificación tarda unos 7 ms y ningún tick del servidor superó los 5 ms mientras aterrizaban
36 pilas.

### 📋 Requisitos
- Minecraft **26.3**
- **Fabric** (con Fabric API) o **NeoForge**
- Instalar **en el cliente y en el servidor** (en un jugador funciona sin más)
- Opcional: Mod Menu

Los cofres de mods que funcionan como contenedores vanilla (18+ espacios) son compatibles automáticamente.
