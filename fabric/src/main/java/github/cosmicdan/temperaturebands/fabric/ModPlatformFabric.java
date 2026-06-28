package github.cosmicdan.temperaturebands.fabric;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import github.cosmicdan.temperaturebands.IModPlatform;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public class ModPlatformFabric implements IModPlatform {
    @Override
    public void registerConfigCommon(final IConfigSpec<ForgeConfigSpec> spec) {
        ForgeConfigRegistry.INSTANCE.register(TemperatureBands.MOD_ID, ModConfig.Type.COMMON, spec);
    }
}
