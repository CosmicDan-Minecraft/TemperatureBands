package github.cosmicdan.temperaturebands.fabric;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import github.cosmicdan.temperaturebands.IModPlatform;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModPlatformFabric implements IModPlatform {
    @Override
    public void registerConfigCommon(ModConfigSpec spec) {
        ConfigRegistry.INSTANCE.register(TemperatureBands.MOD_ID, ModConfig.Type.COMMON, spec);
    }
}
