# Porting QuickStack to another Minecraft version

`main` targets the newest Minecraft (26.3). Every older version range lives on its own branch `mc/<version>` and is
ported **downwards**, each branch starting from the next newer one, so every step only crosses one set of API
changes. Behaviour must stay identical across branches; a fix lands on `main` first and is then ported down.

| Branch | Minecraft | Java |
|---|---|---|
| `main` | 26.3 | 25 |
| `mc/26.2` | 26.2 | 25 |
| `mc/26.1` | 26.1 – 26.1.2 | 25 |
| `mc/1.21.11` | 1.21.11 | 21 |
| `mc/1.21.10` | 1.21.9 – 1.21.10 | 21 |
| `mc/1.21.8` | 1.21.6 – 1.21.8 | 21 |
| `mc/1.21.5` | 1.21.5 | 21 |
| `mc/1.21.4` | 1.21.4 | 21 |
| `mc/1.21.3` | 1.21.2 – 1.21.3 | 21 |
| `mc/1.21.1` | 1.21 – 1.21.1 | 21 |

## Steps

1. **Worktree**: `git worktree add ../QuickStack-ports/<version> -b mc/<version> <newer branch>`.
2. **Build files** from [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) at the branch for the
   target version: `build-logic/`, the root and module `build.gradle` files, `settings.gradle`, the Gradle wrapper and
   the keys in `gradle.properties`. Then put back what QuickStack adds: mod properties, Mod Menu (`compileOnly` +
   `localRuntime`, version for that Minecraft from Modrinth), the `fabricApi.configureTests` block when the client
   gametest API exists, the `-Pselfcheck` run flags, the Java level in the mixin config.
   Versions obfuscated by Mojang (1.21.x) build with official Mojang mappings and the remapping Loom plugin.
3. **Versions**: Fabric Loader and Fabric API from `meta.fabricmc.net` / `maven.fabricmc.net`, NeoForge and NeoForm
   from `maven.neoforged.net`, the newest stable of each for the target.
4. **Sources**: after the first build, unpack the decompiled Minecraft sources that ModDevGradle leaves in
   `common/build/moddev/artifacts/*-sources.jar` (and Fabric API `-sources.jar` files from its Maven) **outside the
   repository**, and check every API against them. Never port from memory.
5. **Port** with the smallest change that keeps behaviour identical. Version-sensitive places:
   - GUI drawing: `IconButton`, `ScreenButtons`, `ConfigScreen`, `FindItemScreen`, `Highlighter`
     (`GuiGraphics` vs `GuiGraphicsExtractor`, `render*` vs `extract*`, blit signatures, 2D pose matrices);
   - input: `AbstractContainerScreenMixin` (method signatures with raw ints vs `MouseButtonEvent`/`KeyEvent`, mouse
     button numbers GLFW 0/1/2 vs SDL 1/2/3, key codes), `KeyMapping` constructor and categories;
   - recipe book: `ServerPlaceRecipeMixin`, `RecipeBookComponentMixin`, `NearbyCrafting` (the placement code and
     `StackedContents` vs `StackedItemContents` changed in 1.21.2);
   - inventory: `Inventory.getNonEquipmentItems()` (1.21.5+) vs `Inventory.items`;
   - names: `Identifier`/`ResourceLocation`, `EntityTypes`/`EntityType`, `minecraft.gui.screen()`/`minecraft.screen`;
   - loader code: Fabric API names before 26.1 follow Yarn (`KeyBindingHelper`, `PayloadTypeRegistry.playC2S()`...),
     NeoForge event and registration names.
6. **Showcase**: delete `QuickStackShowcase`, `ShowcaseOverlay` and the `-Pshowcase` Gradle block; the clips are only
   recorded on `main`.
7. **Verify**, with `JAVA_HOME` pointing at the right JDK:
   - `./gradlew build -x javadoc` for both loaders;
   - `./gradlew :fabric:runClient -Pselfcheck` and `./gradlew :neoforge:runClient -Pselfcheck` must log `[selfcheck] OK`
     (all mixin targets load and transform);
   - `./gradlew :fabric:runClientGameTest` (client gametests exist from 1.21.4) must pass every check; where they do not
     exist, the server-side scenarios become a Fabric server gametest (`fabric-gametest-api-v1`);
   - for a version range, the self-check also on the oldest version of the range.
8. **Metadata**: `fabric.mod.json` and `neoforge.mods.toml` declare the whole range.
9. **Commit** on the branch. Releases are tagged `v<mod version>+<version>`, oldest Minecraft first.
