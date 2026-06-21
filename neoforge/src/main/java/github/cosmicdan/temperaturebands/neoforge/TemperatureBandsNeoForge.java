package github.cosmicdan.temperaturebands.neoforge;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

@Mod(TemperatureBands.MOD_ID)
public final class TemperatureBandsNeoForge {
    private final TemperatureBands INSTANCE;
    public static ModContainer CONTAINER;

    public TemperatureBandsNeoForge(ModContainer container, IEventBus modBus) {
        CONTAINER = container;
        INSTANCE = new TemperatureBands(new ModPlatformNeoForge());
        modBus.addListener(this::onConfigLoad);
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(TemperatureBands.MOD_ID)) {
            if (event.getConfig().getType().equals(ModConfig.Type.COMMON))
                INSTANCE.onConfigLoaded(false);
        }
    }
}
