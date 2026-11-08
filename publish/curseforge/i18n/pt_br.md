## QuickStack — gerenciamento de inventário no estilo Terraria

Aperte **Z** e veja seu saque voar para os baús que já têm aqueles itens. O QuickStack traz para o Minecraft o
*guardar rápido em baús próximos* do Terraria, junto com espaços travados, organização, fabricação a partir de baús
próximos e um punhado de pequenas melhorias que deixam o depósito de baús sem dor de cabeça.

### ✨ Guardar rápido em baús próximos
Aperte **Z** (ou o botão no inventário). O inventário fecha, seus itens saltam na sua frente e voam um por um para os
baús, barris e caixas de Shulker próximos que já contêm o mesmo item. Os baús abrem a tampa quando os itens chegam.

### 🔒 Espaços travados
**Alt + clique** em um espaço para travá-lo. Espaços travados nunca saem do lugar: nem no guardar rápido, nem no
"Guardar tudo", nem na organização. A barra de acesso rápido vem travada por padrão; armadura e mão secundária nunca
são mexidas.

### 📦 Botões do baú
Todo baú, barril e caixa de Shulker ganha uma fileira de botõezinhos:
- **Guardar neste baú** — só os itens que ele já tem
- **Reabastecer** — pegue do baú os itens que você carrega, deixando 1 de cada (configurável)
- **Guardar tudo** / **Pegar tudo**
- **Organizar** o baú
- **Excluir** este baú do guardar em baús próximos

### 🧹 Organização
Organize seu inventário ou qualquer baú com um botão, uma tecla ou um **clique do meio**. Por categoria, nome ou
quantidade. Pilhas incompletas são juntadas.

### 🖱️ Arrastar com Shift
Segure **Shift + botão esquerdo do mouse** e passe por cima dos espaços: cada espaço por onde você passar vai para o
outro inventário, como num Shift + clique. Espaços travados são pulados.

### 🎡 Roda do mouse, um item por vez
**Roda para cima** sobre uma pilha pega mais um item, **roda para baixo** devolve um — como cliques direitos, só que
rápido.

### 🛠️ Fabricação a partir de baús próximos
O livro de receitas conta os itens dos baús ao seu redor e a grade de fabricação pega deles os ingredientes que
faltam. As sobras voltam para o baú de onde vieram quando você fecha a bancada de trabalho.

### 🔍 Encontrar itens
**F6** abre uma grade com tudo o que está guardado nos baús ao seu redor. Digite para filtrar, clique num item e os
baús que o contêm se acendem com uma coluna de brilhos que dá para ver através das paredes.

### ↩️ Desfazer
Guardou a coisa errada? Defina a tecla *Desfazer* e traga de volta o último depósito.

---

### ⚙️ Opções
Tudo pode ser alterado no jogo: pelo botão de engrenagem no inventário, pelo Mod Menu (Fabric) ou pela lista de mods
(NeoForge). No modo um jogador, a segunda aba define os limites do mundo: raio, velocidade de chegada, fabricação a
partir de baús próximos e baús de mods.

### 🚀 Desempenho
Todas as transferências são feitas pelo servidor em pequenos lotes por tick, e os itens voando são só efeito visual
no cliente, então nada pode ser perdido ou duplicado. Testado com **2 352 baús cheios** num raio de 16 blocos: o
planejamento leva cerca de 7 ms e nenhum tick do servidor passou de 5 ms enquanto 36 pilhas chegavam.

### 📋 Requisitos
- Minecraft **26.3**
- **Fabric** (com Fabric API) ou **NeoForge**
- Instale **no cliente e no servidor** (no modo um jogador funciona direto)
- Opcional: Mod Menu

Baús de mods que funcionam como contêineres vanilla (18+ espaços) são suportados automaticamente.
