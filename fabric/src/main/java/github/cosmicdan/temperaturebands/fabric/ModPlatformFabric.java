package github.cosmicdan.temperaturebands.fabric;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import github.cosmicdan.temperaturebands.IModPlatform;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public class ModPlatformFabric implements IModPlatform {
    @Override
    public void registerConfig(ModConfig.Type type, ForgeConfigSpec spec) {
        ForgeConfigRegistry.INSTANCE.register(TemperatureBands.MOD_ID, type, spec);
    }
}
