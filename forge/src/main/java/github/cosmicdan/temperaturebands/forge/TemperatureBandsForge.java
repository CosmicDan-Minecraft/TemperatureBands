package github.cosmicdan.temperaturebands.forge;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TemperatureBands.MOD_ID)
public final class TemperatureBandsForge {
    private final TemperatureBands INSTANCE;

    public TemperatureBandsForge() {
        @SuppressWarnings("removal")
        FMLJavaModLoadingContext context = ModLoadingContext.get().extension();
        INSTANCE = new TemperatureBands(new ModPlatformForge(context));
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::onConfigLoad);
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(TemperatureBands.MOD_ID)) {
            if (event.getConfig().getType().equals(ModConfig.Type.COMMON))
                INSTANCE.onConfigLoaded(false);
        }
    }
}
