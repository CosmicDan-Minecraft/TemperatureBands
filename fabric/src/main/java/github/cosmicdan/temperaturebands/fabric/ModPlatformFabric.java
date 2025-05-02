package github.cosmicdan.temperaturebands.fabric;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import github.cosmicdan.temperaturebands.IModPlatform;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public class ModPlatformFabric implements IModPlatform {
    public ModPlatformFabric() {
    }

    @Override
    public void registerConfig(ModConfig.Type type, IConfigSpec<ForgeConfigSpec> spec) {
        ForgeConfigRegistry.INSTANCE.register(TemperatureBands.MOD_ID, type, spec);
    }
}
