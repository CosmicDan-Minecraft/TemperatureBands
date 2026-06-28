package github.cosmicdan.temperaturebands.forge;

import github.cosmicdan.temperaturebands.IModPlatform;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ModPlatformForge implements IModPlatform {
    private final FMLJavaModLoadingContext context;

    public ModPlatformForge(FMLJavaModLoadingContext context) {
        this.context = context;
    }

    @Override
    public void registerConfigCommon(final IConfigSpec<ForgeConfigSpec> spec) {
        context.registerConfig(ModConfig.Type.COMMON, spec);
    }
}
