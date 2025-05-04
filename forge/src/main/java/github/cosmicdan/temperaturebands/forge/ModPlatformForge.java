package github.cosmicdan.temperaturebands.forge;

import github.cosmicdan.temperaturebands.IModPlatform;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ModPlatformForge implements IModPlatform {
    @Override
    public void registerConfig(final ModConfig.Type type, final ForgeConfigSpec spec) {
        ModLoadingContext.get().registerConfig(type, spec);
    }
}
