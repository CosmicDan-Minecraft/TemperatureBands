package github.cosmicdan.temperaturebands.fabric;


import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import github.cosmicdan.temperaturebands.IModPlatform;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModPlatformFabric implements IModPlatform {
    @Override
    public void registerConfigCommon(ModConfigSpec spec) {
        NeoForgeConfigRegistry.INSTANCE.register(TemperatureBands.MOD_ID, ModConfig.Type.COMMON, spec);
    }

    @Override
    public boolean isClientSide() {
        return FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT);
    }
}
