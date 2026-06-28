package github.cosmicdan.temperaturebands.fabric;

import github.cosmicdan.temperaturebands.IModPlatform;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public class ModPlatformFabric implements IModPlatform {
    @Override
    public void registerConfigCommon(final IConfigSpec<ForgeConfigSpec> spec) {
        ModLoadingContext.registerConfig(TemperatureBands.MOD_ID, ModConfig.Type.COMMON, spec);
    }
}
