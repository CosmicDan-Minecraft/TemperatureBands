package github.cosmicdan.temperaturebands.fabric;

import github.cosmicdan.temperaturebands.IModPlatform;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class ModPlatformFabric implements IModPlatform {
    @Override
    public void registerConfigCommon(final IConfigSpec<ForgeConfigSpec> spec) {
        ModLoadingContext.registerConfig(TemperatureBands.MOD_ID, ModConfig.Type.COMMON, spec);
    }

    @Override
    public boolean isClientSide() {
        return FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT);
    }
}
