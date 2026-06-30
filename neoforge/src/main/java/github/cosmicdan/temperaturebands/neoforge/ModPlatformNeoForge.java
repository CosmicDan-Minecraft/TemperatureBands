package github.cosmicdan.temperaturebands.neoforge;

import github.cosmicdan.temperaturebands.IModPlatform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModPlatformNeoForge implements IModPlatform {
    @Override
    public void registerConfigCommon(ModConfigSpec spec) {
        TemperatureBandsNeoForge.CONTAINER.registerConfig(ModConfig.Type.COMMON, spec);
    }

    @Override
    public boolean isClientSide() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }
}
