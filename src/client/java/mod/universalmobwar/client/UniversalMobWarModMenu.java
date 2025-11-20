package mod.universalmobwar.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import mod.universalmobwar.config.ModConfig;
import net.minecraft.text.Text;

public class UniversalMobWarModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new UniversalMobWarConfigScreen(parent);
    }
}
