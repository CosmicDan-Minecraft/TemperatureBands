package github.cosmicdan.temperaturebands.neoforge;

import github.cosmicdan.temperaturebands.IModPlatform;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModPlatformNeoForge implements IModPlatform {
    @Override
    public void registerConfigCommon(ModConfigSpec spec) {
        TemperatureBandsNeoForge.CONTAINER.registerConfig(ModConfig.Type.COMMON, spec);
    }
}
