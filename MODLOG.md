# QuickStack — MODLOG

Terraria-style quick stack for Minecraft Java: "quick stack to nearby chests" with flying items, chest buttons
(deposit matching / restock keeping N / deposit all / loot all / sort / exclude), locked slots (Alt+click), sorting,
crafting from nearby containers, item search, undo. English + Ukrainian.

## Versions (2026-11-08)
- Minecraft 26.3 (ships unobfuscated, Mojang names, no mappings).
- Java 25: Temurin 25.0.5+8 unpacked to `%USERPROFILE%\.jdks\jdk-25.0.5+8` (system Java is 24; set `JAVA_HOME` to the 25 for Gradle).
- Gradle 9.9.0 (Loom 1.19.4 needs >= 9.8), Fabric Loom 1.19.4 (`net.fabricmc.fabric-loom`), Fabric Loader 0.20.1,
  Fabric API 0.165.2+26.3, Mod Menu 21.0.0 (optional), ModDevGradle 2.0.148, NeoForge 26.3.0.101, NeoForm 26.3-1.
- Template: jaredlll08/MultiLoader-Template branch `26.3` (common + fabric + neoforge, sources compiled into each loader jar).
- Decompiled sources for reading (outside the repo): `E:\Programing\GamesProjects\Minecraft\mc-src-26.3\` (vanilla, neoforge-patched, neoforge-api, fabric-api modules).

## Build / test
- `JAVA_HOME=~/.jdks/jdk-25.0.5+8 ./gradlew build -x javadoc` → `fabric/build/libs/quickstack-fabric-26.3-*.jar`, `neoforge/build/libs/...`.
- `./gradlew :fabric:runClientGameTest` — real client scenarios (`fabric/src/gametest`), screenshots in `fabric/build/run/clientGameTest/screenshots`, log lines `[qs-test] ...: OK|FAIL`.
- `./gradlew :fabric:runClient` — manual dev client (with Mod Menu).

## Architecture
- `common/`: all logic. Loader specifics behind `IPlatformHelper` / `IClientPlatformHelper` (ServiceLoader):
  config dir, sending packets, lock mask + "excluded" flag storage (Fabric data attachments / NeoForge attachment types).
- Server authoritative. `QuickStackManager`: plan at request time (simulated inserts on container copies, nothing
  sent to full containers), items leave the inventory at once and wait in a priority queue keyed by arrival tick;
  at most `maxInsertsPerTick` land per tick; each landing re-checks the container, leftovers return to the player;
  disconnect / stop lands everything. One `Flights` packet per quick stack.
- Container search: block-entity maps of already loaded chunks in range (`getChunkNow`), never loads chunks or scans blocks.
  Double chests = one target. Skips unopened loot chests, locked containers, excluded ones.
- Flying items: client-only `ItemEntity` subclasses with negative ids added to `ClientLevel`, moved each tick
  (vanilla renderer + interpolation). Avoids the renderer API, which changes every version.
- Crafting from nearby: `ServerPlaceRecipeMixin` (count nearby items, pull missing ingredients into the grid);
  client `RecipeBookComponentMixin` adds a synced summary so the recipe book shows recipes as craftable.

## 26.3 API facts (differ from older versions)
- `GuiGraphics` → `GuiGraphicsExtractor`; `render*` → `extract*` (`extractRenderState`, `extractSlot`, `extractContents`).
- `ResourceLocation` → `Identifier`. `EntityType.ITEM` → `EntityTypes.ITEM`. No `BlockPos.getCenter()` (use `Vec3.atCenterOf`).
- `Minecraft.screen`/`setScreen` → `minecraft.gui.screen()` / `minecraft.gui.setScreen(...)`.
- Key codes are SDL scancodes (`InputConstants.KEY_Z = 29`), not GLFW. Use `InputConstants.KEY_*`.
- `KeyMapping.Category` is a record; Fabric registers custom ones with `KeyMapping.Category.register(id)`,
  NeoForge with `RegisterKeyMappingsEvent.registerCategory` (vanilla register is deprecated there).
- Mouse/keys arrive as `MouseButtonEvent` / `KeyEvent` records (`hasAltDown()`, `isConfirmation()`).
- NeoForge client payload handlers: `RegisterClientPayloadHandlersEvent`; server side registers `playToClient(type, codec)` only.

## Log
- 2026-11-08: template → QuickStack, all features written, both loaders compile. First client gametest run.
- 2026-11-08: user feedback round 1. Lock marks made subtle (small faded padlock). Crafting from nearby now keeps
  a per-player loan ledger: borrowed ingredients go back to their containers when the grid is cleared
  (ServerPlaceRecipe.clearGrid) or the menu closes (AbstractContainerMenu.clearContainer), player's own items count
  as used first. Search is a grid screen of everything in containers within searchRadius (RequestStorage /
  StorageSummary), matches not stored shown faded; search results get a rising END_ROD column. Inventory buttons
  moved to a tab above the window (ClientConfig.buttonPlacement TAB/INSIDE). Mouse buttons in 26.x are SDL
  numbers (left = 1, middle = 2): fixed. Gametest: 36/36.
- 2026-11-08: round 2. Container screens: no tab; container actions on the title row, inventory actions on the
  "Inventory" row (tab stays for the inventory / crafting table / furnace). New quick-stack icon (arrow splitting
  into two chests). Planning indexes only the item kinds the player can stack. Stress test: radius 16, 2352 chests
  holding 61 152 full stacks, full inventory: scan 5 ms, plan 16 ms cold / 7 ms warm, search summary 17 ms, worst tick
  while landing 4.8 ms. Gametest 40/40.
- 2026-11-08: round 3. Shift sweep (default Shift + left button; option Shift only / off) via mouseDragged and
  slotClicked(QUICK_MOVE); when Shift is held but the mouse event lacks the modifier, the first slot is moved by the
  mod. Mouse wheel: up = TakeFromSlot payload (one item onto the cursor, predicted on the client), down = vanilla
  right click (PICKUP button 1). Container click buttons stay 0/1 in the protocol even though SDL mouse buttons are 1/3.
  Gametest 43/43.
- 2026-11-08: crash fix. At 1920x1080 with GUI scale 4 (270 units high) the settings column ran off screen; a widget
  below the edge gets a zero-height scissor and 26.3's GuiRenderer throws "Scissor size must be >0". ConfigScreen now
  has two tabs (preferences / world) and lays widgets out in as many columns as the height needs. Highlighter lights
  both halves of a double chest (client looks up the partner from the block state). Gametest 45/45, incl. GUI scale 4.
- 2026-11-08: removed the template's `pack.mcmeta` (pack_format 8): both loaders describe mod resources themselves,
  and on 1.21.x Fabric it made the game log "Removed resource pack quickstack ... no longer compatible". The
  self-check now waits for the loading overlay to close and also requires the mod's translations to be loaded.
- 2026-11-08: release 0.1.0 for every range, one branch each (oldest first): mc/1.21.1 (1.21 – 1.21.1),
  mc/1.21.3 (1.21.2 – 1.21.3), mc/1.21.4, mc/1.21.5, mc/1.21.8 (1.21.6 – 1.21.8), mc/1.21.10 (1.21.9 – 1.21.10),
  mc/1.21.11, mc/26.1 (26.1 – 26.1.2), mc/26.2, main (26.3). Before 1.21.4 there is no client gametest API: those
  branches run 36 server gametests (`:fabric:runGameTest`) instead of the 45 client checks.
