package github.cosmicdan.temperaturebands.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TemperatureBands.MOD_ID)
public final class TemperatureBandsForge {
    private final TemperatureBands INSTANCE;

    public TemperatureBandsForge(FMLJavaModLoadingContext context) {
        INSTANCE = new TemperatureBands(new ModPlatformForge(context));
    }
}
