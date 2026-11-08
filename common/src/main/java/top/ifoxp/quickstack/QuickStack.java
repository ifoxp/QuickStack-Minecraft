package top.ifoxp.quickstack;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ifoxp.quickstack.config.ClientConfig;
import top.ifoxp.quickstack.config.ServerConfig;

public final class QuickStack {

    public static final String MOD_ID = "quickstack";
    public static final String MOD_NAME = "QuickStack";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private QuickStack() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Called by each loader's entrypoint on both sides. */
    public static void init() {
        ServerConfig.load();
        LOG.info("QuickStack loaded");
    }

    /** Called by each loader's client entrypoint. */
    public static void initClient() {
        ClientConfig.load();
    }
}
