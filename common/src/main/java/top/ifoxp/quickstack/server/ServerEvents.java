package top.ifoxp.quickstack.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import top.ifoxp.quickstack.config.ServerConfig;

/** Server lifecycle hooks; each loader forwards its own events here. */
public final class ServerEvents {

    private ServerEvents() {
    }

    public static void onServerStarting(MinecraftServer server) {
        ServerConfig.load();
    }

    public static void onServerTick(MinecraftServer server) {
        QuickStackManager.tick(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        LockManager.sync(player);
    }

    /** Respawn and dimension change create a new player object on the client; resend the locks. */
    public static void onPlayerRespawn(ServerPlayer player) {
        LockManager.sync(player);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        QuickStackManager.flush(player.serverLevel().getServer(), player.getUUID());
        NearbyCrafting.forget(player.getUUID());
    }

    public static void onServerStopping(MinecraftServer server) {
        QuickStackManager.flushAll(server);
    }
}
