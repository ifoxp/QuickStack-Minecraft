# QuickStack

Terraria-style inventory management for Minecraft Java Edition (Fabric and NeoForge).

- **Quick stack to nearby containers** (button in the inventory or the `Z` key): the inventory closes, your items
  burst out in front of the camera and fly into the nearby chests, barrels and shulker boxes that already hold the
  same item. Chests open their lids as the items land.
- **Locked slots**: Alt+click a slot to lock it; locked slots are never stacked, deposited or sorted. The hotbar is
  locked by default; armour and offhand are never touched.
- **Container buttons**: quick stack into this container, restock from it (keeps 1 of each item by default, configurable),
  deposit all, loot all, sort, and exclude this container from quick stack to nearby.
- **Sorting** of the inventory and containers (button, middle click or a key), by category, name or count.
- **Crafting from nearby containers**: the recipe book counts items in containers around you and takes missing
  ingredients from them.
- **Find an item** in nearby containers: the containers holding it sparkle.
- **Undo** the last quick stack (key, unbound by default).
- English and Ukrainian.

The mod must be installed on the server (singleplayer included). All transfers are done by the server, in small
batches per tick, so a big quick stack does not lag it.

## Settings
In game: the gear button in the inventory, Mod Menu (Fabric) or the Mods list (NeoForge).
Files: `config/quickstack-client.json` (your preferences) and `config/quickstack-server.json` (radius, landing rate,
cooldown, crafting from nearby, modded containers).

## Building
Java 25 is required for Minecraft 26.3.

```
./gradlew build -x javadoc            # jars in fabric/build/libs and neoforge/build/libs
./gradlew :fabric:runClient           # dev client
./gradlew :fabric:runClientGameTest   # scripted scenarios in a real client, with screenshots
```

Project layout follows the MultiLoader template: `common/` holds the logic, `fabric/` and `neoforge/` are thin
loader adapters.
