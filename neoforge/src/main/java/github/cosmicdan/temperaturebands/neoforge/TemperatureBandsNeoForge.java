package github.cosmicdan.temperaturebands.neoforge;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(TemperatureBands.MOD_ID)
public final class TemperatureBandsNeoForge {
    private final TemperatureBands INSTANCE;
    public static ModContainer CONTAINER;

    public TemperatureBandsNeoForge(ModContainer container, IEventBus modBus) {
        CONTAINER = container;
        INSTANCE = new TemperatureBands(new ModPlatformNeoForge());
    }
}
