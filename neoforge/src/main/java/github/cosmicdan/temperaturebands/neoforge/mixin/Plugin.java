package github.cosmicdan.temperaturebands.neoforge.mixin;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class Plugin implements IMixinConfigPlugin {
    private static boolean doneLogMsg = false;

    @Override
    public void onLoad(String s) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (targetClassName.startsWith("com.caeruleusTait.world.preview.")) {
            if (LoadingModList.get().getModFileById("world_preview") == null) {
                return false;
            } else {
                if (!doneLogMsg) {
                    TemperatureBands.LOGGER.info("World Preview detected; applying benchmark-related hooks. Please report if you see a mixin error after this line, it means World Preview has changed and benchmark may no longer work properly.");
                    doneLogMsg = true;
                }
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
