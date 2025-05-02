package github.cosmicdan.temperaturebands.forge;

import github.cosmicdan.temperaturebands.IModPlatform;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ModPlatformForge implements IModPlatform {
    private final FMLJavaModLoadingContext context;

    public ModPlatformForge(FMLJavaModLoadingContext contextIn) {
        context = contextIn;
    }

    @Override
    public void registerConfig(final ModConfig.Type type, final IConfigSpec<ForgeConfigSpec> spec) {
        context.registerConfig(type, spec);
    }
}
