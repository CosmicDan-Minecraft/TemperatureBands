package github.cosmicdan.temperaturebands;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public interface IModPlatform {
    void registerConfig(ModConfig.Type type, IConfigSpec<ForgeConfigSpec> spec);
}
