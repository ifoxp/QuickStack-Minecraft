package top.ifoxp.quickstack.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import top.ifoxp.quickstack.QuickStack;

/**
 * Development check, off unless the JVM runs with {@code -Dquickstack.selfcheck=true} (the {@code -Pselfcheck}
 * Gradle flag): once the client is up, load every class QuickStack mixes into, so a mixin whose target changed in
 * this Minecraft version fails right away (injectors require their targets), then quit. It needs no world, so it
 * works the same on every loader and version.
 */
public final class SelfCheck {

    private static final List<String> MIXIN_TARGETS = List.of(
            "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
            "net.minecraft.client.gui.screens.inventory.InventoryScreen",
            "net.minecraft.client.gui.screens.inventory.ContainerScreen",
            "net.minecraft.client.gui.screens.inventory.CraftingScreen",
            "net.minecraft.client.gui.screens.recipebook.RecipeBookComponent",
            "net.minecraft.client.MouseHandler",
            "net.minecraft.recipebook.ServerPlaceRecipe",
            "net.minecraft.world.inventory.AbstractContainerMenu",
            "net.minecraft.world.CompoundContainer");

    private static boolean done;

    private SelfCheck() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("quickstack.selfcheck");
    }

    /** Runs once, on the first client tick (rendering and registries are ready by then). */
    public static void tick(Minecraft minecraft) {
        if (done) {
            return;
        }
        done = true;
        int loaded = 0;
        for (String name : MIXIN_TARGETS) {
            try {
                Class.forName(name, true, SelfCheck.class.getClassLoader());
                loaded++;
            } catch (Throwable e) {
                QuickStack.LOG.error("[selfcheck] FAIL loading {}", name, e);
            }
        }
        QuickStack.LOG.info("[selfcheck] {} / {} mixin targets loaded and transformed", loaded, MIXIN_TARGETS.size());
        if (loaded == MIXIN_TARGETS.size()) {
            QuickStack.LOG.info("[selfcheck] OK");
        }
        minecraft.stop();
    }
}
