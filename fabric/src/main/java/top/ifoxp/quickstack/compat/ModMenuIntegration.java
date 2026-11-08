package top.ifoxp.quickstack.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import top.ifoxp.quickstack.client.ui.ConfigScreen;

/** Opens QuickStack's settings from Mod Menu's mod list (only loaded when Mod Menu is installed). */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
